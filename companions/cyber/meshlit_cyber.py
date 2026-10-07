#!/usr/bin/env python3
"""Original read-only assessment companion. No third-party tools bundled."""
import argparse, hashlib, json, os, shutil, struct, subprocess, sys, zipfile
from pathlib import Path
MAX_INPUT=512*1024*1024
TOOLS={'androguard':'androguard','quark':'quark','tshark':'tshark','objection':'objection','frida':'frida','drozer':'drozer','metasploit':'msfconsole','sleuthkit':'fls','autopsy':'autopsy'}
def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''): h.update(chunk)
    return h.hexdigest()
def analyze(tool,path,expected):
    path=Path(path)
    if not path.is_file() or path.stat().st_size>MAX_INPUT: raise ValueError('Input missing or exceeds 512 MiB')
    actual=digest(path)
    if actual.lower()!=expected.lower(): raise ValueError('Input digest mismatch')
    if tool=='apk_inventory':
        with zipfile.ZipFile(path) as archive:
            entries=archive.infolist()
            if len(entries)>20000: raise ValueError('ZIP entry budget exceeded')
            names=[item.filename for item in entries]
            if any(name.startswith('/') or '..' in Path(name).parts for name in names): raise ValueError('Unsafe archive paths')
            if 'AndroidManifest.xml' not in names: raise ValueError('Not an APK')
            if sum(item.file_size for item in entries)>2*1024**3: raise ValueError('Expanded archive budget exceeded')
            findings={'entry_count':len(entries),'manifest_present':True,'dex_files':[n for n in names if n.endswith('.dex')][:100],'native_libraries':[n for n in names if n.startswith('lib/') and n.endswith('.so')][:500],'signature_entries':[n for n in names if n.startswith('META-INF/')][:100]}
            coverage='ZIP metadata only; no binary manifest decoding, signature verification or malware verdict'
    elif tool=='pcap_summary':
        magic={b'\xd4\xc3\xb2\xa1':('<',1000000),b'\xa1\xb2\xc3\xd4':('>',1000000),b'\x4d\x3c\xb2\xa1':('<',1000000000),b'\xa1\xb2\x3c\x4d':('>',1000000000)}
        with path.open('rb') as stream:
            header=stream.read(24)
            if len(header)!=24 or header[:4] not in magic: raise ValueError('Only classic PCAP supported')
            endian,scale=magic[header[:4]];major,minor,_,_,snap,link=struct.unpack(endian+'HHiIII',header[4:])
            if (major,minor)!=(2,4) or not 1<=snap<=16*1024*1024: raise ValueError('Invalid PCAP header')
            packets=total=0
            while True:
                record=stream.read(16)
                if not record: break
                if len(record)!=16: raise ValueError('Truncated packet header')
                seconds,fraction,captured,original=struct.unpack(endian+'IIII',record)
                if fraction>=scale or captured>snap or captured>original: raise ValueError('Invalid packet limits')
                if len(stream.read(captured))!=captured: raise ValueError('Truncated packet')
                packets+=1;total+=captured
                if packets>1000000: raise ValueError('Packet budget exceeded')
            findings={'packets':packets,'captured_bytes':total,'link_type':link,'timestamp_resolution':scale};coverage='Record structure only; no payload export, decryption or protocol verdict'
    elif tool=='tshark':
        binary=shutil.which('tshark')
        if not binary: raise ValueError('tshark is not installed')
        # Count-only analysis: raw packet payloads never enter the report.
        with __import__('tempfile').TemporaryFile() as output:
            proc=subprocess.Popen([binary,'-n','-r',str(path),'-c','10000','-T','fields','-e','frame.number','-e','frame.len'],stdout=output,stderr=subprocess.DEVNULL,start_new_session=True)
            try: code=proc.wait(timeout=40)
            except BaseException:
                os.killpg(proc.pid,__import__('signal').SIGKILL);proc.wait();raise
            if code!=0: raise ValueError('tshark analysis failed')
            output.seek(0);data=output.read(1024*1024+1)
            if len(data)>1024*1024: raise ValueError('Output limit exceeded')
            lines=data.decode().splitlines();findings={'packets_inspected':len(lines),'packet_limit':10000};coverage='Bounded count-only tshark analysis; capture may contain more packets'
    else: raise ValueError('Adapter not implemented')
    if digest(path)!=actual: raise ValueError('Input changed during analysis')
    return {'schema':1,'tool':tool,'status':'completed','input_sha256':actual,'findings':findings,'coverage':coverage}
def main():
    parser=argparse.ArgumentParser();sub=parser.add_subparsers(dest='action',required=True)
    sub.add_parser('probe');run=sub.add_parser('run');run.add_argument('--tool',choices=['apk_inventory','pcap_summary','tshark'],required=True);run.add_argument('--input',required=True);run.add_argument('--sha256',required=True)
    args=parser.parse_args()
    try:
        value={'tools':{name:{'installed':shutil.which(binary) is not None,'adapter_implemented':name=='tshark'} for name,binary in TOOLS.items()}} if args.action=='probe' else analyze(args.tool,args.input,args.sha256)
        print(json.dumps(value));return 0
    except Exception:
        print(json.dumps({'status':'failed','error':'Input, digest, installed tool or bounded analysis check failed'}));return 1
if __name__=='__main__': sys.exit(main())

import argparse
import os
import pathlib
import re
import sys
import time
import urllib.request

READ_CHUNK_SIZE = 1024*1024
spinner = ['-', '/', '|', '\\', '-', '/', '|', '\\']
def fetch(URL, token, name_only = False):
    request = urllib.request.Request(URL)
    request.add_header("Authorization", "Bearer " + token)
    with (urllib.request.urlopen(request) as response):
        filename = None
        disposition = response.getheader('Content-Disposition')
        if disposition:
            filename = re.findall("filename=(.+)", disposition)[0]
        if filename:
            filename = os.path.basename(filename)
        else:
            filename = "downloaded.nc"
        content_length_hr = human_readable_size(int(response.getheader('Content-Length')))
        content_downloaded = 0
        print(f"Fetching {URL} to {filename} 0/{content_length_hr}", end="", flush=True)
        spin_index = 0
        last_rate_time = time.time()
        last_rate_downloaded = 0
        mbs = ""
        if name_only:
            print()
            return
        with open(filename, "wb") as fd:
            while True:
                spin_index = (spin_index + 1) % len(spinner)
                data = response.read(READ_CHUNK_SIZE)
                if data:
                    fd.write(data)
                    content_downloaded += len(data)
                    diff = (content_downloaded-last_rate_downloaded)/(1024*1024)
                    # update every 20M change
                    if diff > 20:
                        now = time.time()
                        mbs = f"{diff/(now-last_rate_time):.1f} MB/s"
                        progress = f"{human_readable_size(content_downloaded)}/{content_length_hr}"
                        print(f"\rFetching {URL} to {filename} {spinner[spin_index]} {progress} {mbs}     ", end="", flush=True)
                        last_rate_time = now
                        last_rate_downloaded = content_downloaded
                else:
                    break
        print("\b\b\b.")


def human_readable_size(size):
    if size is None:
        return "unknown"
    if size < 1024:
        return f"{size}b"
    elif size < 1024 * 1024:
        return f"{size/1024:.1f}k"
    elif size < 1024 * 1024 * 1024:
        return f"{size/(1024*1024):.1f}m"
    else:
        return f"{size/(1024*1024*1024):.1f}g"

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        prog="wsFetch",
        description="Fetch files from WildStore using an access token."
    )
    parser.add_argument("URL", nargs="+", help="the URL to download or the name of a file containing a list of URLs to download")
    cf = str(pathlib.Path.home() / ".config" / "wildstore.ini")
    parser.add_argument("--token-file", default=cf,
                        help=f"location of ini file (default is {cf}) with access token. (token=XXXX)")
    parser.add_argument('--name-only', action='store_true', help='Display filename to download, but do not download', default=False)
    args=parser.parse_args()
    token = None
    try:
        with open(args.token_file, "r") as f:
            for line in f.readlines():
                parts = line.split("=", 1)
                if (len(parts) == 2 and parts[0].strip() == "token"):
                    token = parts[1].strip()
    except Exception as e:
        print(e, file=sys.stderr)

    if not token:
        print(f"no token defined in {args.token_file}. make sure there is a line of the form token=XXXXX")
        exit(2)

    for U in args.URL:
        if not U.startswith("http"):
            print(f"Using {U} as a file with URLS:")
            try:
                with open(U, "r") as f:
                    for line in f.readlines():
                        fetch(line.strip(), token, args.name_only)
            except Exception as e:
                print(e, file=sys.stderr)
        else:
            fetch(U, token, args.name_only)

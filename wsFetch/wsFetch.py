import argparse
import os
import pathlib
import re
import sys
import urllib.request

READ_CHUNK_SIZE = 1024*1024
spinner = ['-', '/', '|', '\\', '-', '/', '|', '\\']
def fetch(URL, token):
    request = urllib.request.Request(URL)
    request.add_header("Authorization", "Bearer " + token)
    print(request.full_url)
    print(f"Fetching {URL} ", end="", flush=True)
    with (urllib.request.urlopen(request) as response):
        filename = None
        disposition = response.getheader('Content-Disposition')
        if disposition:
            filename = re.findall("filename=(.+)", disposition)[0]
        if filename:
            filename = os.path.basename(filename)
        else:
            filename = "downloaded.nc"
        print(f"to {filename} ", end="", flush=True)
        spin_index = 0
        with open(filename, "wb") as fd:
            while True:
                print(f"\b{spinner[spin_index]}", end="", flush=True)
                spin_index = (spin_index + 1) % len(spinner)

                data = response.read(READ_CHUNK_SIZE)
                if data:
                    fd.write(data)
                else:
                    break
        print("\b.")


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
                        fetch(line.strip(), token)
            except Exception as e:
                print(e, file=sys.stderr)
        else:
            fetch(U, token)

import argparse
import pathlib
import sys
import urllib.request


def fetch(URL, token):
    request = urllib.request.Request(URL)
    request.add_header("Authorization", "Bearer " + token)
    print(request.full_url)
    print(f"Fetching {URL} ", end="", flush=True)
    with urllib.request.urlopen(request) as response:
        response.read()
        print(response.headers)


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        prog="wsFetch",
        description="Fetch files from WildStore using an access token."
    )
    parser.add_argument("URL", nargs="+")
    cf = str(pathlib.Path.home() / ".config" / "wildstore.ini")
    parser.add_argument("--token", default=cf,
                        help=f"location of ini file (default is {cf} with access token. (token=XXXX)")
    args=parser.parse_args()
    token = None
    try:
        with open(args.token, "r") as f:
            for line in f.readlines():
                parts = line.split("=", 1)
                if (len(parts) == 2 and parts[0].strip() == "token"):
                    token = parts[1].strip()
    except Exception as e:
        print(e, file=sys.stderr)
    for U in args.URL:
        if not U.startswith("http"):
            print(f"Using {U} as a file with URLS:")
            try:
                with open(U, "r") as f:
                    for line in f.readlines():
                        fetch(line.strip(), args.token)
            except Exception as e:
                print(e, file=sys.stderr)
        else:
            fetch(U, args.token)

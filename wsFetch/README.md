# wsFetch: simple python script to retrieve files using bearer token

```
usage: wsFetch [-h] [--token-file TOKEN_FILE] URL [URL ...]

Fetch files from WildStore using an access token.

positional arguments:
  URL                   the URL to download or the name of a file containing a list of URLs to download

options:
  -h, --help            show this help message and exit
  --token-file TOKEN_FILE
                        location of ini file (default is /<homedir>/.config/wildstore.ini) with access token. (token=XXXX)
```

This script simplifies the downloading of multiple URLs using an access token, also called a bearer token.
The token can be generated on the wildstore website for the user who would like to do the download.

To use wsFetch create a "token file" with the line

```
token=XXXX
```

where XXXX is your token.
If you store this file in the default location (shown by running `python3 wsFetch.py --help`), you do not need to specify the `--token`
argument when running the script.

The `URL` parameter of the script can be a URL you wish to download, or if the parameter does not start with `http`, `wsFetch` will interpret the parameter as a file of URLs.
The files should have a URL on each line, and `wsFetch` will download each URL in order.

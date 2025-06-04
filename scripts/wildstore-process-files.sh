#!/bin/bash

JAVA=/usr/java/jdk-17/bin/java

# store the directory of thise script in BASE_DIR
BASE_DIR=$(dirname "$(readlink -f "$0")")

# create a temporary file that will be deleted when script ends
TEMP_FILE=$(mktemp "$BASE_DIR/wildstore-XXXXXX.txt")
# trap to delete the temporary file on exit
trap 'rm -f "$TEMP_FILE"' EXIT

SHARE=false
# process options:
# --help and display help
# --share set the SHARE variable to true
# --config set the CONFIG_FILE variable to the passed filename
# and store the rest of the arguments in an array of filenames
while [[ $# -gt 0 ]]; do
    case "$1" in
        --help)
            echo "Usage: $0 [--share] --config <config file> [--dir <output_dir>] <file1> <file2> ..."
            echo "  the config file must be specified. it has one line:"
            echo "token=<your token from the wildstore web interface>"
            echo "  if output_dir is specified, the files will be created as REGION/DAY/TYPE"
            exit 1
            ;;
        --share)
            SHARE=true
            shift
            ;;
        --dir)
            OUTDIR="$2"
            shift 2
            ;;
        --config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        # catch any other arguments that start with a -
        -*)
            echo "Unknown option: $1"
            echo "Use --help for usage information."
            exit 1
            ;;
        *)
            # canonicalize the filename in $1
            CANONICAL="$(readlink -f "$1")"
            # if CANONICAL is empty, print an error
            if [ -z "$CANONICAL" ]; then
                echo "Error: Invalid file '$1'."
            else
                # print an error if the file does not end with .nc
                if [[ ! "$CANONICAL" =~ \.nc$ ]]; then
                    echo "'$1' is not an .nc file. skipping."
                else
                    echo "$CANONICAL" >> "$TEMP_FILE"
                    FILENAMES+=("$CANONICAL")
                fi
            fi
            shift
            ;;
    esac
done

# check if FILENAMES have been passed
if [ ${#FILENAMES[@]} -eq 0 ]; then
    echo "No files specified. Use --help for usage information."
    exit 2
fi

# check if there is a line that starts with "token=" in the config file if share is set
if [ -z "$CONFIG_FILE" ]
then
    echo "Error: --config must be specified when using --share."
    exit 3
fi

if [ ! -f "$CONFIG_FILE" ]
then
    echo "Error: Config file '$CONFIG_FILE' does not exist."
    exit 4
fi

TOKEN=$(grep '^token=' "$CONFIG_FILE" | cut -d'=' -f2-)
if [ -z "$TOKEN" ]
then
    echo "Error: Config file '$CONFIG_FILE' must contain a line starting with 'token='."
    exit 5
fi

$JAVA -jar $BASE_DIR/wildfirestorage-crawler.jar \
    --parallelism=8 \
    --hostname=http://127.0.0.1:27777 \
    --configFile "$CONFIG_FILE" \
    "$TEMP_FILE"

if [ "$SHARE" == "true" ]
then
    # look at each of the files in FILENAMES
    for filename in "${FILENAMES[@]}":
    do
        # get the file name of filename
        filename_only=$(basename "$filename")
        if [ -n "$OUTDIR" ]
        then
            # split filename_only using -
            IFS='-' read -ra parts <<< "$filename_only"
            # the two filename examples are:
            # forecasts: fmda-CONUS-20250502-09-f24.nc
            # analysis: fmda-CONUS-20250502-09.nc
            # if parts has 4 elements, it is an analysis file
            if [ ${#parts[@]} -lt 4 ]
            then
                echo "looking for file names of the form 'fmda-REGION-DATE-HOUR-*.nc"
            else
                REG="${parts[1]}"
                DATE="${parts[2]}"
                if [ ${#parts[@]} -gt 4 ]
                then
                    TYPE=forecast
                else
                    TYPE=analysis
                fi
            fi
            mkdir -p "$OUTDIR/$REG/$DATE"
            OUTFILE="$OUTDIR/$REG/$DATE/$TYPE.lst"
        fi
        if [ -n "$OUTFILE" ] && grep $filename "$OUTFILE" &> /dev/null
        then
            echo "File '$filename' already exists in '$OUTFILE'. Skipping."
            continue
        fi
        # the first line of output of the share is the email address we are sharing with, so we skip it
        URL="$($JAVA -jar $BASE_DIR/wildfirestorage-cli.jar share \
            --email=\* \
            --token=$TOKEN \
            --validFor=year "$filename" | tail -n +2)?filename=$filename_only"
        if [ -n "$OUTFILE" ]
        then
            echo "Adding file $filename to '$OUTFILE'."
            echo "$URL" >> "$OUTFILE"
        else
            echo "$URL"
        fi
    done
fi
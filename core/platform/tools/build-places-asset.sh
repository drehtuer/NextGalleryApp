#!/bin/sh
# Rebuilds the bundled place list (D45) from the GeoNames dump. Run by hand when the place data should be refreshed,
# and commit the result; the app build does not download anything.
#
# Keeps only the columns PlaceFileParser reads: name, country code, latitude, longitude, population.
# After a refresh, update EXPECTED_ROWS in AssetPlaceSourceTest with the row count this script prints.
#
# Data: GeoNames (https://www.geonames.org), licensed under CC BY 4.0.
set -eu

url="https://download.geonames.org/export/dump/cities15000.zip"
target="$(dirname "$0")/../src/main/assets/geonames/cities15000.tsv"

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

curl --fail --silent --show-error --location --max-time 300 --output "$workdir/cities.zip" "$url"
unzip -q "$workdir/cities.zip" cities15000.txt -d "$workdir"

mkdir -p "$(dirname "$target")"
{
    printf '# GeoNames cities15000 (https://www.geonames.org), CC BY 4.0, downloaded %s.\n' "$(date -u +%Y-%m-%d)"
    printf '# Reduced to the columns name, country code, latitude, longitude, population.\n'
    # In the dump, columns 2, 9, 5, 6 and 15 are name, country code, latitude, longitude and population.
    LC_ALL=C awk -F '\t' -v OFS='\t' '{ print $2, $9, $5, $6, $15 }' "$workdir/cities15000.txt"
} > "$target"

echo "Wrote $target: $(grep -c -v '^#' "$target") places"

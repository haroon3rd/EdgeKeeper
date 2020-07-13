# Common configuration for all certificates 
#  Edit these fields to be appropriate for your organization
#  If they are left blank, they will not be included.  Do not leave COUNTRY
#  blank (you may set it to "XX" if you want to be obtuse).

COUNTRY=US
STATE=
CITY=
ORGANIZATION=LENSS
ORGANIZATIONAL_UNIT=

CAPASS=${CAPASS:-atakatak}
PASS=${PASS:-$CAPASS}

## subdirectory to put all the actual certs and keys in
DIR=files

### delete this line once you have edited the above fields
# echo "Please edit cert-metadata.sh before running this script!"; exit -1



##### don't edit below this line #####

SUBJBASE="/C=${COUNTRY}/"
if [ -n "$STATE" ]; then
 SUBJBASE+="ST=${STATE}/"
fi
if [ -n "$CITY" ]; then
 SUBJBASE+="L=${CITY}/"
fi
if [ -n "$ORGANIZATION" ]; then
 SUBJBASE+="O=${ORGANIZATION}/"
fi
if [ -n "$ORGANIZATIONAL_UNIT" ]; then
 SUBJBASE+="OU=${ORGANIZATIONAL_UNIT}/"
fi


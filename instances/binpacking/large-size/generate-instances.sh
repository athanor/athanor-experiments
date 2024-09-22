# usage: ./generate-instances.sh <settingFile> <outDir> <prefix>
# example: ./generate-instances.sh large1.dat n1000 n1000-
./bppgen <$1
python converter.py $2 $3

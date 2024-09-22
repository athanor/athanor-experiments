# usage: python convertToLns.py <outDir> <prefix>
# ex: python convertToLns.py n1000 n1000-
import sys
import os

outDir = sys.argv[1]
prefix = sys.argv[2]

if os.path.isdir(outDir) is False:
    os.mkdir(outDir)

with open("SDAT", "rt") as f:
    data = f.readlines()

f = open("SDAT", "r+")
instId = 0
lines = f.readlines()
lineId = 0
while True:

    # read an instance
    if lineId == len(lines):
        break
    line = lines[lineId]
    lineId += 1
    if "BPP" not in line:
        print("Error: BPP not found")
        sys.exit(1)
    nEntries = int(lines[lineId])
    lineId += 1
    capacity = int(lines[lineId])
    lineId += 1
    weights = []
    nItems = 0
    for i in range(nEntries):
        s = lines[lineId].strip().split(" ")
        lineId += 1
        s = list(filter(lambda x: x != "", s))
        weight = int(s[0])
        nItemsOfThisWeight = int(s[1])
        weights.extend([weight] * nItemsOfThisWeight)
        nItems += nItemsOfThisWeight
    
    # write instance to .lns file
    lnsFile = f"{outDir}/{prefix}{instId}.lns"
    with open(lnsFile, "wt") as f:
        f.write(f"{nItems}\n")
        f.write(f"{nItems}\n")
        f.write(f"{capacity}\n")
        f.write(" ".join([str(w) for w in weights]))

    # write instance to .param file
    paramFile = f"{outDir}/{prefix}{instId}.param"
    with open(paramFile, "wt") as f:
        s = "letting items be new type enum {" + ",".join([f"i{i+1}" for i in range(nItems)]) + "}\n"
        f.write(s)
        s = "letting weights be function (" + ",".join([f"i{i+1}-->{weights[i]}" for i in range(nItems)]) + ")\n"
        f.write(s)
        f.write(f"letting binSize be {capacity}")
    
    # write instance to .mzn file
    mznFile = f"{outDir}/{prefix}{instId}.dzn"
    with open(mznFile, "wt") as f:
        f.write(f"num_stuff = {nItems};\n")
        f.write("num_bins = num_stuff;\n")
        f.write("stuff = [" + ",".join([str(w) for w in weights]) + "];")
        f.write(f"bin_capacity = {capacity};")

    instId += 1

f.close()

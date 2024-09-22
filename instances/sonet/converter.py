from math import *
from os.path import *
import sys

import glob
import subprocess
import shlex
import os
import json
lastLine=""

def run_cmd(cmd, printOutput=False, outFile=None):
    lsCmds = shlex.split(cmd)
    p = subprocess.run(lsCmds, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    output = p.stdout.decode("utf-8")
    if outFile is not None:
        with open(outFile, "wt") as f:
            f.write(output)
    if printOutput:
        print(output)
    return output, p.returncode

def error(message):
    print(f"Error: {sys.argv[0]}:\n{message}", file=sys.stderr)
    sys.exit(1)


def read_param_file(fileName: str): 
    cmd = f"conjure pretty --output-format=json {fileName}"
    sdata, rc = run_cmd(cmd)
    if rc!=0:
        print(f"ERROR: {cmd}")
        sys.exit(1)
    sdata = sdata.replace("Parsing as a parameter file","")
    data = json.loads(sdata)
    return data


def write_to_dzn(data, f):
    print(f"nnodes = {data['nnodes']};",file=f)
    print(f"nrings = {data['nrings']};",file=f)
    print(f"capacity = {data['capacity']};",file=f)
    print("demand = [|",file=f)
    for i in range(1, data["nnodes"]+1):
        for j in range(1, data["nnodes"]+1):
            if i<j and [i,j] in data["demand"]:
                print("1", end='', file=f)
            else:
                print("0", end='', file=f)

            if i<data["nnodes"] or j<data["nnodes"]:
                print(",", end='', file=f)
        if i<data["nnodes"]:
            print("|", file=f)

    print("|];", file=f)


def write_to_lns(data, f):
    print(f"{data['nnodes']} {data['nrings']} {data['capacity']}", file=f)
    for d in data["demand"]:
        print(f"{d[0]} {d[1]}", file=f)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        error("Usage: python converter.py <essenceParamInputFile> <outputFile>. \nOutput can be one of the following: Choco LNS (.lns), MiniZinc (.dzn)")

    paramFile = sys.argv[1]
    if paramFile.split(".")[-1]!="param":
        error("Input file must be in .param (Essence) format")

    data = read_param_file(paramFile)

    outputFile = sys.argv[2]
    outputType = outputFile.split(".")[-1]
    if outputType not in ["lns","dzn"]:
        error(f"Output format {outputType} not supported")
    with open(sys.argv[2], "wt") as f:
        if outputType=="dzn":
            write_to_dzn(data,f)
        else:
            write_to_lns(data,f)

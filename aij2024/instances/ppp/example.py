import numpy as np
import subprocess

rng = np.random.default_rng()

generatorScript = "./generator/PPPGenerator.jar"

nInsts = 100

minBoats, maxBoats = 10, 80
minPeriods, maxPeriods = 5, 30
minCapacityUB, maxCapacityUB = 10, 100
#minCrewSizeRatio, maxCrewSizeRatio = 0.2, 0.9

for i in range(nInsts):
    seed = rng.integers(10, 99999)
    nBoats = rng.integers(minBoats, maxBoats+1)
    nPeriods = rng.integers(minPeriods, maxPeriods+1)
    capacityUB = rng.integers(minCapacityUB, maxCapacityUB+1)
    cmd = f"java -jar {generatorScript} {seed} {nBoats} {nPeriods} {capacityUB}"
    fn = f"inst-{nBoats}-{nPeriods}-{capacityUB}.param"
    rc = subprocess.call(cmd.split(" "), stdout=open(fn,"wt")) 
    if rc!=0:
        print(f"ERROR: {cmd}")
    			

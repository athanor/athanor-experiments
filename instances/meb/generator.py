#!/usr/bin/env python3
#generated according to spec in 
#http://www.csplib.org/Problems/prob048/
import sys
from math import *
from random import *

def error(message):
    print("Error:", sys.argv[0], ": ", message, file=sys.stderr)
    sys.exit(1)

def calcDistance(node1,node2):
    return hypot(node2[0]-node1[0],node2[1]-node1[1])

def calcPower(node1,node2, exp):
    return (calcDistance(node1,node2) ** exp) * 0.0001

#NGUYEN: update generation procedure to guarantee feasibility
def generateDevicePositions(maxRange, numberDevices, maxPower,exp):
    maxSquareDistance = int((float(maxPower)/0.0001) ** (2.0/exp))
    maxDistance = sqrt(maxSquareDistance)

    devices = [(randint(0,maxRange),randint(0,maxRange))]
    while len(devices) < numberDevices:
        refDevice = devices[randint(0,len(devices)-1)]
        #print(str(max(0,refDevice[0]-maxDistance)) + ',' + str(min(maxRange,refDevice[0]+maxDistance)))
        x = randint(ceil(max(0,refDevice[0]-maxDistance)), int(min(maxRange,refDevice[0]+maxDistance)))
        signY = [-1,1][randint(0,1)]
        y = refDevice[1] + sqrt(maxSquareDistance - (x-refDevice[0])**2) * signY
        newDevice = (x,y)
        devices.append(newDevice)
        
    return devices

def printPowers(devices,  maxPower, exp):
    print("letting linkCosts be function (", end="")
    first = True
    for (i,d1) in enumerate(devices):
        for (j,d2) in enumerate(devices):
            if first:
                first = False
            else:
                print(",")
            power = calcPower(d1,d2,exp)
            if i == j or power > maxPower:
                print("(" + str(i+1) + "," + str(j+1) + ")", "-->", 0, end="")
            else:
                print("(" + str(i+1) + "," + str(j+1) + ")", "-->", int(power), end="")
    print(")")


def main(squareLength,numberDevices,maxPower,exp,randomSeed):
    seed(randomSeed)
    devices = generateDevicePositions(squareLength,numberDevices, maxPower,exp)
    print("""$Random instance generated in accordance to specification at
    $http://www.csplib.org/Problems/prob048/""")
    print("$Generated " + str(numberDevices) + " devices in a " + 
    str(squareLength) + "*" + str(squareLength) + 
    " area with a max power of " + str(maxPower) + 
    " and a exp constant of " + str(exp))
    print("letting numberNodes be", numberDevices)
    print("letting initialNode be ", randint(1,numberDevices))
    printPowers(devices,maxPower,exp)



if __name__ == "__main__":
    if len(sys.argv) != 6:
        error("Expected 5 parameters: square_length number_devices max_power exp_constant random_seed") 
    main(int(sys.argv[1]), int(sys.argv[2]), float(sys.argv[3]), float(sys.argv[4]), int(sys.argv[5]))

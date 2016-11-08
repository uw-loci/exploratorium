# -*- coding: utf-8 -*-
"""
Python code to detect fish eggs with OpenCV circular Hough transform function.
V1.0 Nov 4th, 2016
@Author: Bing Dai (bdai6@wisc.edu)
"""

import cv2
import argparse
import numpy as np
import timeit

"""
# construct the argument parser and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-i", "--image", required = True, help = "Path to the image")
args = vars(ap.parse_args())

# load the image, clone it for output, and then convert it to grayscale
img = cv2.imread(args["image"])
output = img.copy()
#gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
"""

start = timeit.timeit()  # start timing

# import image
#img = cv2.imread('../images/junkfinder_25x_dic_frame1.jpg')  # 3 eggs
#img = cv2.imread('../images/fish2.jpg')  # 4 eggs. need to reduce minRadius to 25
#img = cv2.imread('../images/Kip2201.jpg')  # 1.5 eggs
img = cv2.imread('../images/Kip1401.jpg')  # air bubble that cannot be eliminated
#img = cv2.imread('../images/bubble2.jpg')  # air bubbles
#img = cv2.imread('../images/bubble3.jpg')  # air bubbles
#output = img.copy()
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)  # convert to grayscale
#img = cv2.medianBlur(img,5)
#cv2.imshow('original image',img)

#Shrink image size to accelarate computation. Only works for grayscale image.
# Calculate the ratio of the new image to the original image
# make sure the aspect ratio doesn't change
resizeRatio = 200.0 / img.shape[1]  # scale image width to 200 pixels
dim = (200, int(img.shape[0] * resizeRatio))
# perform the actual resizing of the image
resized = cv2.resize(gray, dim, interpolation = cv2.INTER_AREA)
#cv2.imshow("resized", resized)
#retval,BW = cv2.threshold(resized,127,255,cv2.THRESH_BINARY)
#cv2.imshow("binary",BW)

# Detect circles (fish eggs) in the image
def checkforBubbles(imageIn, circlesIn, threshold):
    "Eliminate false positives (air bubbles, debris, etc) from results. Basic idea is to get a binary mask around edge of circles and check if the inner boundary is dark - if dark then bubbles."
    circlesOut = np.uint16(np.zeros(circlesIn.shape))
    
    # Create a dummy BW image of the same size as original image imgIn
    retval,BW = cv2.threshold(imageIn,127,255,cv2.THRESH_BINARY)
    # set all pixels of BW inside hough circle to 1, otherwise 0
    BW = np.zeros(imageIn.shape, dtype="unit8")
    # Creates a r=2 disk structuring element
    # apply erosion on the BW and generate a tmp image
    # generate a ring (= BW - tmp) that only the pixels on the Hough circle edge are 1
    # apply ring as a binary mask on imgIn to get imageTmp
    # calculate the mean value of imageTmp
    # if meanValue > bubbleThreshold then it means features on the inner boundary are not black, then keep the data point; 
    # otherwise remove it
    # update the results
    return circlesOut;
    
"""
cv2.HoughCircles(image, method, dp, minDist)

image: 8-bit, single channel image. If working with a color image, convert to grayscale first.
method: Defines the method to detect circles in images. Currently, the only implemented method is cv2.HOUGH_GRADIENT, which corresponds to the Yuen et al. paper.
dp: This parameter is the inverse ratio of the accumulator resolution to the image resolution (see Yuen et al. for more details). Essentially, the larger the dp gets, the smaller the accumulator array gets.
minDist: Minimum distance between the center (x, y) coordinates of detected circles. If the minDist is too small, multiple circles in the same neighborhood as the original may be (falsely) detected. If the minDist is too large, then some circles may not be detected at all.
param1: Gradient value used to handle edge detection in the Yuen et al. method.
param2: Accumulator threshold value for the cv2.HOUGH_GRADIENT method. The smaller the threshold is, the more circles will be detected (including false circles). The larger the threshold is, the more circles will potentially be returned.
minRadius: Minimum size of the radius (in pixels).
maxRadius: Maximum size of the radius (in pixels).
"""
#circles = cv2.HoughCircles(resized,cv2.HOUGH_GRADIENT,1,50,
#                            param1=50,param2=30,minRadius=10,maxRadius=30)
# circles is a 3D numpy array whose shpae is (1, n, 3) 
# i.e., (datatype, number of circles detected, x/y/radius of the circle)
circles = cv2.HoughCircles(resized,cv2.HOUGH_GRADIENT,1,50, param1=50,param2=30,minRadius=35,maxRadius=50)
# BGR colors
white = (255,255,255)
red = (0,0,255)

# Eliminate air bubbles from list of circles
# First ensure at least some circles were found
if circles is not None:
    # convert the (x, y) coordinates and radius of the circles to integers
    circles = np.uint16(np.around(circles))  # (column #, row #, radius)
    # initialize
    #circlesFiltered = np.uint16(np.empty(circles.shape))
    circlesFiltered = np.uint16(np.zeros(circles.shape))
    threshold = 70
    eggNum = 0
    for circle in circles[0,:]:
        mask = np.zeros(resized.shape,dtype="uint8")
        cv2.circle(mask,(circle[0],circle[1]),circle[2],white,-1)
        #retval,mask = cv2.threshold(tmp,10,255,cv2.THRESH_BINARY)
        se = cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(5,5))  # circular structure element with radius 2
        eroded = cv2.erode(mask,se,iterations = 1)
     #   cv2.imshow('eroded',eroded)
        mask = cv2.bitwise_xor(mask,eroded) # only edge pixels of the circle remain unchnaged.
        #print "mask weight:",np.sum(mask), "mask maximum:",np.max(mask)
    #    cv2.imshow('mask', mask)
        # keep only the pixels on the Hough circle boundary unchanged
        ring = cv2.bitwise_and(resized,mask)
    #    cv2.imshow('result',test)
    #    cv2.waitKey(0)
        averagePixelWeight = np.sum(ring)/np.sum(mask/255) # set mask max pixel value to 1
        print "Average pixel weight:",averagePixelWeight
        # features on the inner boundary are not black
        if averagePixelWeight > threshold:
            circlesFiltered[0,eggNum,] = circle
            eggNum +=1
            print "Circle #", eggNum, "is a fish egg."    
        else:
            print "Circle #", eggNum, "is an air bubble."    
    print "Found",eggNum, "fish eggs."

    # remove all rows with zero values
    # np.all() in this case reduces the 3D array (1,n,3) to (n,3)
    # Use np.array() to change it back to (1,n,3) shape but caused error in line (x,y,r) = ...
    #circlesFiltered = np.array(circlesFiltered[~np.all(circlesFiltered==0, axis=2)],dtype="uint16")  # disabled until figure out the issue...
    fishEggs = circlesFiltered[~np.all(circlesFiltered==0, axis=2)]  
   
    # ensure at least some fish eggs were found
    if fishEggs.shape[0] > 0:    
        for fishEgg in fishEggs:
            # convert center and radius back to original image scale
            (x,y,r) = np.uint16(np.around(np.dot(fishEgg, 1/resizeRatio)))  # column #, row #, radius
            # draw the outer circle on original image
            print 'Center:',x,y,'Radius:',r
            cv2.circle(img,(x,y),r,red,4)
            # draw the center of the circle
            cv2.circle(img,(x,y),2,red,5)

        cv2.imshow('detected fish eggs',img)
        #cv2.imshow('Marks on BW image',BW)
        #cv2.imshow('detected fish eggs',np.hstack([img, output]))
        cv2.waitKey(0)
        cv2.destroyAllWindows()

else:
    print 'No eggs found!'

end = timeit.timeit()
print 'Time (seconds) to process one image:', end-start

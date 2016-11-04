# -*- coding: utf-8 -*-
"""
Python code to detect fish eggs with OpenCV circular Hough transform function.
V1.0 Nov 4th, 2016
@Author: Bing Dai (bdai6@wisc.edu)
"""

import cv2
import argparse
import numpy as np

# construct the argument parser and parse the arguments
#ap = argparse.ArgumentParser()
#ap.add_argument("-i", "--image", required = True, help = "Path to the image")
#args = vars(ap.parse_args())
#
## load the image, clone it for output, and then convert it to grayscale
#img = cv2.imread(args["image"])
#output = img.copy()
#gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# import image as grayscale
img = cv2.imread('../images/junkfinder_25x_dic_frame1.jpg',0)
#img = cv2.imread('../images/Kip2201.jpg',0)
#img = cv2.medianBlur(img,5)
#cv2.imshow('original image',img)

# we need to keep in mind aspect ratio so the image does
# not look skewed or distorted -- therefore, we calculate
# the ratio of the new image to the old image
r = 200.0 / img.shape[1]
dim = (200, int(img.shape[0] * r))
 
# perform the actual resizing of the image and show it
resized = cv2.resize(img, dim, interpolation = cv2.INTER_AREA)
#cv2.imshow("resized", resized)

# Detect circles (fish eggs) in the image
#circles = cv2.HoughCircles(resized,cv2.HOUGH_GRADIENT,1,50,
#                            param1=50,param2=30,minRadius=10,maxRadius=30)
circles = cv2.HoughCircles(resized,cv2.HOUGH_GRADIENT,1,50,
                            param1=20,param2=30,minRadius=30,maxRadius=50)

# ensure at least some circles were found
if circles is not None:
    # convert the (x, y) coordinates and radius of the circles to integers
    circles = np.uint16(np.around(circles))
    
    for i in circles[0,:]:
        # draw the outer circle
        print i
        cv2.circle(resized,(i[0],i[1]),i[2],(0,255,0),2)
        # draw the center of the circle
        cv2.circle(resized,(i[0],i[1]),2,(0,0,255),3)
    
    cv2.imshow('detected fish eggs',resized)
    cv2.waitKey(0)
    cv2.destroyAllWindows()
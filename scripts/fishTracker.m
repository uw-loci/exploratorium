%Author - Guneet Singh Mehta,
%email - gmehta2@wisc.edu
%Graduate of Indian Institute of Technology Jodhpur
%Author: Bing Dai
%Email: bdai6@wisc.edu

%% Code based on Guneet's original fishTracker1.m
% this version focuses on the core functions, i.e., read an image, scale 
% down the image, locate the center and radius of fish eggs.
% Optional: annote on the image with circles, and output the annoted image.

% Read the video
videoFilename='C:\Users\LOCI\Documents\Exploratorium\Seeing_Scientifically_Share_with_LOCI\videos_junkNheartFinder\junkfinder_25x_dic.mp4';
[pathstr,fileName,ext] = fileparts(videoFilename);
vid=VideoReader(videoFilename);
numFrames = vid.NumberOfFrames;  % maybe obsolete in the future
%numFrames = 2801;
f1=figure;
% radius for the zebrafish for different magnification the number needs to be scaled accordingly.
% need to find a way to automatically adjust the values when changing the zoom.
% maybe using DMA to record the zoom info from hardware feedback?  -- BD
imgSetting.imSize = 200;  % downsize the frame to less than 200
imgSetting.lowerRadius = 10;  % imfindcircles() setting for 2.5x objective
imgSetting.higherRadius = 40;
imgSetting.objectPolarity = 'dark';
imgSetting.sensitivity = 0.9;
imgSetting.threshold = 80;  % threshold for checkForBubble()

for i=1:200:numFrames
    % Read each frame
    frame=read(vid,i);
    %Crop the image to remove the left and right column
    frame=frame(179:825,129:1132,:);%for junkfinder_25x_dic file
    tic
    [centers, radii] = findEggs(frame,imgSetting);
    toc
%     %downsample the frame so that the dimension is less than - reduce
%     %by same factor to retain the aspect ratio
%     reducedFrame = scaleImage(frame,imgSetting.imSize);
%     imshow(reducedFrame,'Border','tight');
%     %use a method similar to the shirts project to find the mask
%     [centers, radii] = imfindcircles(reducedFrame,[imgSetting.lowerRadius imgSetting.higherRadius],'ObjectPolarity',imgSetting.objectPolarity,'Sensitivity',imgSetting.sensitivity);
%     [centers, radii] = checkForBubble(centers,radii,reducedFrame,imgSetting.threshold);
%     h = viscircles(centers,radii);

%   Save the annoted image
    annotedFrame = getframe(f1);
    annotedImg = annotedFrame.cdata;
    fprintf('%d\n',i);
    imwrite(annotedImg,[fileName '_frame' num2str(i) '.jpg']);  % each processed frame is saved as a separate image. In future the anotation (mark) should be directly superimposed to video frame. -- BD
end
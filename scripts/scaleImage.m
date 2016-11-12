function imgOut = scaleImage(imgIn, imSize)
% Downsize image size to less than imSize
% Bing Dai. bdai6@wisc.edu

[s1,s2,~]=size(imgIn);
downSampleFactor=max(ceil(s1/imSize),ceil(s2/imSize));
imRows=ceil(s1/downSampleFactor);
imCols=ceil(s2/downSampleFactor);
imgOut=imresize(imgIn,[imRows imCols]);
end
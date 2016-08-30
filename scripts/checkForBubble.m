function[cFinal,rFinal]=checkForBubble(cIn,rIn,frame,threshold)
%Author - Guneet Singh Mehta,
%Research Assistant Laboratory for Optical and Computational
%Instrumentation
%University of Wisconsin Madison.
%email - gmehta2@wisc.edu
%Graduate of Indian Institute of Technology Jodhpur

    %eliminates the bubble circles
    %Basi working - get the mask and see if the inner boundary is dark- if
    %dark then bubble
    sCircles=size(cIn,1);
    [s1,s2,~]=size(frame);
    BW=zeros(s1,s2);
    count=1;
%    threshold=80;
    se=strel('disk',2);
    % pre-allocate array size to accelarate computation
    cFinal=zeros(size(cIn));
    rFinal=zeros(size(rIn));
    if(isempty(cIn)==1)
       return; 
    end
    for k=1:sCircles
        c=cIn(k,:);
        r=rIn(k,:);
        for m=1:s1
            for n=1:s2
                if((m-c(2))^2+(n-c(1))^2<=r*r)
                   BW(m,n)=1; 
                end
            end
        end
        %figure;imagesc(BW);
%        BW3=repmat(BW,[1 1 3]);
        %figure;imshow(frame.*uint8(BW3));
        temp=imerode(BW,se);
        ring=BW-temp;
        gray=rgb2gray(frame);
        temp=gray.*uint8(ring);
        meanValue=sum(temp(:))/sum(ring(:));
        if(meanValue>=threshold)
            cFinal(count,:)=cIn(k,:);
            rFinal(count,:)=rIn(k,:);
            count=count+1;
        end
    end
    cFinal(~any(cFinal,2),:) = [];  % remove all rows with zero values
    rFinal(~any(rFinal,2),:) = [];  % remove all rows with zero values
    
end
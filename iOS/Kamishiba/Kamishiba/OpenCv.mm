//
//  OpenCv.m
//  Kamishiba
//
//  Created by huser on 2018/08/08.
//  Copyright © 2018年 WallStudio. All rights reserved.
//

//
//  OpenCvFilter.m
//  GekigaCamera
//
//  Created by hirauchi.shinichi on 2017/02/19.
//  Copyright © 2017年 SAPPOROWORKS. All rights reserved.
//

// ref. https://qiita.com/gun534/items/042bcbaa7bc5051bec09
#import <opencv2/opencv.hpp>
#import <opencv2/imgcodecs/ios.h>

#import <UIKit/UIKit.h>
#import "Kamishiba-Bridging-Header.h"

using namespace std;
using namespace cv;

class FeaturedImage{
    Ptr<AKAZE> akaze;
    Mat mat;
public:
    Ptr<vector<cv::KeyPoint>> keyPoints;
    Ptr<Mat> descriptors;
    FeaturedImage(const cv::Mat& image){
        akaze = cv::AKAZE::create();
        mat = image;
        cvtColor(mat, mat, CV_BGR2GRAY);
        // サイズが大きいとなぜかUIImageが全て描画されなくなってしまう
        resize(mat, mat, cv::Size(200, 100));
        keyPoints = new vector<KeyPoint>();
        descriptors = new Mat();
        akaze->detectAndCompute(mat, noArray(), *keyPoints, *descriptors);
        drawKeypoints(mat, *keyPoints, mat);
    }
    FeaturedImage(const NSString& path){
        
    }
    ~FeaturedImage(){
        mat.release();
        keyPoints.release();
        descriptors.release();
        akaze.release();
    }
    cv::Mat getMat(){
        return mat;
    }
};

@implementation OpenCv : NSObject

- (id) init {
    return self;
}

-(FeaturedImage *)enFeatured:(UIImage *)image {
    cv::Mat mat;
    UIImageToMat(image, mat);
    FeaturedImage *fi = new FeaturedImage(mat);
    return fi;
}

-(UIImage *)Filter:(UIImage *)image{
    // 方向を修正
    UIGraphicsBeginImageContext(image.size);
    [image drawInRect:CGRectMake(0, 0, image.size.width, image.size.height)];
    image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    //UIImageをcv::Matに変換
    cv::Mat mat;
    UIImageToMat(image, mat);
    FeaturedImage fi(mat);
    
    return MatToUIImage(fi.getMat());
}

@end


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

@implementation OpenCv : NSObject

- (id) init {
    return self;
}

-(UIImage *)Filter:(UIImage *)image {
    
    // 方向を修正
    UIGraphicsBeginImageContext(image.size);
    [image drawInRect:CGRectMake(0, 0, image.size.width, image.size.height)];
    image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    //UIImageをcv::Matに変換
    cv::Mat mat;
    UIImageToMat(image, mat);
    cv::cvtColor(mat,mat,CV_BGR2GRAY);
    
    return MatToUIImage(mat);
}

@end


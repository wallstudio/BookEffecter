//
//  ViewController.swift
//  Kamishiba
//
//  Created by huser on 2018/08/08.
//  Copyright © 2018年 WallStudio. All rights reserved.
//

import UIKit

class ViewController: UIViewController, AVCaptureDelegate {
    
    @IBOutlet weak var imageView: UIImageView!
    
    let avCapture = AVCapture()
    let openCv = OpenCv()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        avCapture.delegate = self
        // Do any additional setup after loading the view, typically from a nib.
    }
    
    func capture(image: UIImage) {
        imageView.image = openCv.filter(image)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}


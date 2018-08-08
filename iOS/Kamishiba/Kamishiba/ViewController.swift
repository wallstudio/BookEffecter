//
//  ViewController.swift
//  Kamishiba
//
//  Created by huser on 2018/08/08.
//  Copyright © 2018年 WallStudio. All rights reserved.
//

import Foundation
import UIKit

class ViewController: UIViewController, AVCaptureDelegate {
    
    @IBOutlet weak var imageView: UIImageView!
    @IBOutlet weak var matchPreviewView: UIImageView!
    
    @IBOutlet weak var titleLabel: UIOutlineLabel!
    @IBOutlet weak var pageLabel: UIOutlineLabel!
    @IBOutlet weak var autherLabel: UIOutlineLabel!
    
    let avCapture = AVCapture()
    let openCv = OpenCv()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        avCapture.delegate = self
        // Do any additional setup after loading the view, typically from a nib.
        
    }
    
    func capture(image: UIImage) {
        imageView.image = image
        let featureds = NSMutableArray(capacity: 0)
        let filted = openCv.filter(image)
        matchPreviewView.image = filted
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}
// ref. http://ofsilvers.hateblo.jp/entry/uilabel-with-stroke
class UIOutlineLabel: UILabel {
    @IBInspectable var strokeSize: CGFloat = 100
    @IBInspectable var strokeColor: UIColor = UIColor.white
    
    override func drawText(in rect: CGRect) {
        // stroke
        let cr = UIGraphicsGetCurrentContext()
        let textColor = self.textColor
        
        cr!.setLineWidth(self.strokeSize)
        cr!.setLineJoin(.round)
        cr!.setTextDrawingMode(.stroke)
        self.textColor = self.strokeColor
        super.drawText(in: rect)
        
        cr!.setTextDrawingMode(.fill)
        self.textColor = textColor
        super.drawText(in: rect)
    }
}

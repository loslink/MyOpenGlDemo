//原始颜色
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES texture;
varying vec2 v_TexCoordinate;
void main () {
      vec4 textureColor = texture2D(texture, v_TexCoordinate);
      gl_FragColor = textureColor;
}





//图像上下颠倒
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//uniform samplerExternalOES texture;
//varying vec2 v_TexCoordinate; //纹理坐标0 - 1
//void main () {
//     vec4 textureColor = texture2D(texture, vec2(v_TexCoordinate.x, 1.0 - v_TexCoordinate.y));
//     gl_FragColor = textureColor;
//}




//变成黑白
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);
//uniform samplerExternalOES texture;
//varying vec2 v_TexCoordinate;
//void main () {
//      vec4 textureColor = texture2D(texture, v_TexCoordinate);
//      float luminance = dot(textureColor.rgb, W);
//      gl_FragColor = vec4(vec3(luminance), textureColor.a);
////      gl_FragColor = vec4(vec3(textureColor.g), textureColor.a);"+//Gray=G；
////      gl_FragColor = vec4((vec3((textureColor.r+textureColor.g+textureColor.b)/3.0)), textureColor.a);"+ //Gray=(R+G+B)/3;
//}



//漩涡
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//const float PI = 3.14159265;
//uniform samplerExternalOES texture;//和第一行配合
//const float uD = 80.0; //旋转角度
//const float uR = 0.5; //旋转半径
//varying vec2 v_TexCoordinate;
//
//void main()
//{
//      ivec2 ires = ivec2(512, 512);
//      float Res = float(ires.s);
//
//      vec2 st = v_TexCoordinate;
//      float Radius = Res * uR;
//
//      vec2 xy = Res * st;
//
//      vec2 dxy = xy - vec2(Res/2., Res/2.);
//      float r = length(dxy);
//
//      float beta = atan(dxy.y, dxy.x) + radians(uD) * 2.0 * (-(r/Radius)*(r/Radius) + 1.0);//(1.0 - r/Radius);
//
//      vec2 xy1 = xy;
//      if(r<=Radius)
//      {
//            xy1 = Res/2. + r*vec2(cos(beta), sin(beta));
//      }
//
//      st = xy1/Res;
//
//      vec3 irgb = texture2D(texture, st).rgb;
//
//      gl_FragColor = vec4( irgb, 1.0 );
//}


//马赛克
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//
//varying vec2 v_TexCoordinate;
//uniform samplerExternalOES image;
//const vec2 TexSize = vec2(400.0, 400.0);
//const vec2 mosaicSize = vec2(8.0, 8.0);
//
//void main()
//{
//    vec2 intXY = vec2(v_TexCoordinate.x*TexSize.x, v_TexCoordinate.y*TexSize.y);
//    vec2 XYMosaic = vec2(floor(intXY.x/mosaicSize.x)*mosaicSize.x, floor(intXY.y/mosaicSize.y)*mosaicSize.y);
//    vec2 UVMosaic = vec2(XYMosaic.x/TexSize.x, XYMosaic.y/TexSize.y);
//    vec4 color = texture2D(image, UVMosaic);
//    gl_FragColor = color;
//}




//浮雕效果，效果不理想
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//varying vec2 v_TexCoordinate;
//uniform samplerExternalOES image;
//const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);
//const vec2 TexSize = vec2(100.0, 100.0);
//const vec4 bkColor = vec4(0.5, 0.5, 0.5, 1.0);
//
//void main()
//{
//    vec2 tex = v_TexCoordinate;
//    vec2 upLeftUV = vec2(tex.x-1.0/TexSize.x, tex.y-1.0/TexSize.y);
//    vec4 curColor = texture2D(image, v_TexCoordinate);
//    vec4 upLeftColor = texture2D(image, upLeftUV);
//    vec4 delColor = curColor - upLeftColor;
//    float luminance = dot(delColor.rgb, W);
//    gl_FragColor = vec4(vec3(luminance), 0.0) + bkColor;
//}
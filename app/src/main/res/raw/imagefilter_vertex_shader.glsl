uniform mat4 uMVPMatrix;
attribute vec4 position;
attribute vec4 inputTextureCoordinate;
attribute vec4 inputTextureCoordinate2;
 
varying vec2 textureCoordinate;
varying vec2 textureCoordinate2;
 
void main()
{
    gl_Position = position * uMVPMatrix;//顶点坐标
    textureCoordinate = inputTextureCoordinate.xy;//纹理坐标
    textureCoordinate2 = inputTextureCoordinate2.xy;
}

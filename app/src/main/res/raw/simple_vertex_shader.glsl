attribute vec4 vPosition; //顶点数组，向量4维（x,y,z,w）
attribute vec4 vTexCoordinate; //纹理数组
uniform mat4 textureTransform;
varying vec2 v_TexCoordinate;
void main() {
     v_TexCoordinate = (textureTransform * vTexCoordinate).xy;
     gl_Position = vPosition;
}
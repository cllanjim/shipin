package com.laifeng.sopcastsdk.video;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author AleXQ
 * @Date 16/3/15
 * glsl 文件转换成 字符串
 * <p>
 * glsl文件 ：：：用来在OpenGL中着色编程的语言，也即开发人员写的短小的自定义程序
 * GLSL（GL Shading Language）的着色器代码分成2个部分：Vertex Shader（顶点着色器）和Fragment（片断着色器）
 * 负责运行顶点着色的是顶点着色器。它可以得到当前OpenGL 中的状态，
 * GLSL内置变量进行传递。GLSL其使用C语言作为基础高阶着色语言，避免了使用汇编语言或硬件规格语言的复杂性。
 */

public class GLSLFileUtils {

    public static String getFileContextFromAssets(Context context, String fileName) {
        String fileContent = "";
        try {
            InputStream is = context.getAssets().open(fileName);
            fileContent = inputStream2String(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileContent;
    }

    public static String inputStream2String(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

}

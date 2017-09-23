package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

@TargetApi(18)
public class GlUtil {
	private static final String TAG = "GlUtil";

	public static FloatBuffer createSquareVtx() {
		final float vtx[] = {
				// XYZ, UV
				-1f,  1f, 0f, 0f, 1f,
				-1f, -1f, 0f, 0f, 0f,
				1f,   1f, 0f, 1f, 1f,
				1f,  -1f, 0f, 1f, 0f,
		};
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);//创建4倍的 vtx[] 长度字节的缓冲区
		// 根据平台支持的字节顺序的常量。 检索基础平台的本机字节顺序。
		// 此方法的定义使得对性能敏感的 Java 代码能分配字节顺序与硬件相同的直接缓冲区。本机代码库通常在使用此类缓冲区时更为高效。
		//BIG_ENDIAN  表示 big-endian 字节顺序的常量。按照此顺序，多字节值的字节顺序是从最高有效位到最低有效位的。
		//LITTLE_ENDIAN 表示 little-endian 字节顺序的常量。按照此顺序，多字节值的字节顺序是从最低有效位到最高有效位的。
		bb.order(ByteOrder.nativeOrder());
		//创建此字节缓冲区的视图，作为 float 缓冲区。
		//新缓冲区的内容将从此缓冲区的当前位置开始。此缓冲区内容的更改在新缓冲区中是可见的，反之亦然；这两个缓冲区的位置、界限和标记值是相互独立的。
		//新缓冲区的位置将为零，其容量和其界限将为此缓冲区中剩余字节数的四分之一，其标记是不确定的。当且仅当此缓冲区为直接时，新缓冲区才是直接的，当且仅当此缓冲区为只读时，新缓冲区才是只读的。
		FloatBuffer fb = bb.asFloatBuffer();
		//相对批量 put 方法（可选操作）。
		//此方法将给定源 float 数组中的所有内容传输到此缓冲区中。调用此方法的形式为 dst.put(a)，该调用与以下调用完全相同：
		fb.put(vtx);
		//设置此缓冲区的位置。如果标记已定义并且大于新的位置，则要丢弃该标记。
		fb.position(0);
		return fb;
	}

	public static FloatBuffer createVertexBuffer() {
		final float vtx[] = {
				// XYZ
				-1f,  1f, 0f,
				-1f, -1f, 0f,
				1f,   1f, 0f,
				1f,  -1f, 0f,
		};
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(vtx);
		fb.position(0);
		return fb;
	}

	public static FloatBuffer createTexCoordBuffer() {
		final float vtx[] = {
				// UV
				0f, 1f,
				0f, 0f,
				1f, 1f,
				1f, 0f,
		};
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(vtx);
		fb.position(0);
		return fb;
	}

	public static float[] createIdentityMtx() {
		float[] m = new float[16];
		Matrix.setIdentityM(m, 0);
		return m;
	}
	//处理着色器 创建连接
	public static int createProgram(String vertexSource, String fragmentSource) {
		//加载定点着色器  创建着色器
		int vs = loadShader(GLES20.GL_VERTEX_SHADER,   vertexSource);
		//加载片元着色器
		int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

		// 创建程序容器并连接
		int program = GLES20.glCreateProgram();
		// 向程序中加入顶点着色器
		GLES20.glAttachShader(program, vs);
		// 向程序中加入片元着色器
		GLES20.glAttachShader(program, fs);
		// 链接程序
		GLES20.glLinkProgram(program);
		int[] linkStatus = new int[1];
		//连接阶段使用glGetProgramiv获取连接情况
		/**
		 * 函数原型：
		 void glGetProgramiv (int program, int pname, int[] params, int offset)
		 参数含义：
		 program是一个着色器程序的id；
		 pname是GL_LINK_STATUS；
		 param是返回值，如果一切正常返回GL_TRUE代，否则返回GL_FALSE。
		 */
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
		// 若链接失败则报错并删除程序
		if (linkStatus[0] != GLES20.GL_TRUE) {
			SopCastLog.e(TAG, "Could not link program:");
			//连接阶段使用glGetProgramiv获取连接情况
			SopCastLog.e(TAG, GLES20.glGetProgramInfoLog(program));
			GLES20.glDeleteProgram(program);
			program = 0;
		}
		return program;
	}
	//编译着色器
	public static int loadShader(int shaderType, String source) {
		// 创建一个新shader
		/**
		 * 函数原型：
		 int glCreateShader (int type)
		 方法参数：
		 GLES20.GL_VERTEX_SHADER          (顶点shader)
		 GLES20.GL_FRAGMENT_SHADER        (片元shader)
		 如果调用成功的话，函数将返回一个整形的正整数作为shader容器的id
		 */
		int shader = GLES20.glCreateShader(shaderType);
		// 加载shader源代码
		/**
		 * 函数原型：
		 void glShaderSource (int shader, String string)
		 参数含义：
		 shader是代表shader容器的id（由glCreateShader返回的整形数）；
		 string是包含源程序的字符串数组。
		 */
		GLES20.glShaderSource(shader, source);
		// 编译shader
		/**
		 * 函数原型：
		 void glCompileShader (int shader)
		 参数含义：
		 shader是代表shader容器的id。
		 */
		GLES20.glCompileShader(shader);
		// 存放编译成功shader数量的数组
		int[] compiled = new int[1];
		// 获取Shader的编译情况
		/**
		 * \函数原型：
		 void glGetShaderiv (int shader, int pname, int[] params, int offset)
		 参数含义：
		 shader是一个shader的id；
		 pname使用GL_COMPILE_STATUS；
		 params是返回值，如果一切正常返回GL_TRUE代，否则返回GL_FALSE。
		 */
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			//若编译失败则显示错误日志并删除此shader
			SopCastLog.e(TAG, "Could not compile shader(TYPE=" + shaderType + "):");
			/**
			 * 编译阶段使用glGetShaderInfoLog获取编译错误
			 * 函数原型：
			 String glGetShaderInfoLog (int shader)
			 参数含义：
			 shader是一个顶点shader或者片元shader的id。
			 */
			SopCastLog.e(TAG, GLES20.glGetShaderInfoLog(shader));
			GLES20.glDeleteShader(shader);
			shader = 0;
		}
		//
		return shader;
	}

	public static void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			SopCastLog.e(TAG, op + ": glGetError: 0x" + Integer.toHexString(error));
			throw new RuntimeException("glGetError encountered (see log)");
		}
	}

	public static void checkEglError(String op) {
		int error;
		while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
			SopCastLog.e(TAG, op + ": eglGetError: 0x" + Integer.toHexString(error));
			throw new RuntimeException("eglGetError encountered (see log)");
		}
	}
}

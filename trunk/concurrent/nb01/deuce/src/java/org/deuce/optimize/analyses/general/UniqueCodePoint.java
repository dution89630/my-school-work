package org.deuce.optimize.analyses.general;

import java.util.ArrayList;
import java.util.List;

import soot.AbstractJasminClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;

public class UniqueCodePoint {
	String className;
	String methodName;
	String signature;
	int bytecodeOffset;

	@Override
	public String toString() {
		if (bytecodeOffset == 0) {
			return "UniqueCodePoint [className=" + className + ", methodName="
					+ methodName + ", signature=" + signature + "]";
		} else {
			return "UniqueCodePoint [bytecodeOffset=" + bytecodeOffset
					+ ", className=" + className + ", methodName=" + methodName
					+ ", signature=" + signature + "]";
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bytecodeOffset;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime * result
				+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result
				+ ((signature == null) ? 0 : signature.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		UniqueCodePoint other = (UniqueCodePoint) obj;
		if (bytecodeOffset != other.bytecodeOffset) {
			return false;
		}
		if (className == null) {
			if (other.className != null) {
				return false;
			}
		} else if (!className.equals(other.className)) {
			return false;
		}
		if (methodName == null) {
			if (other.methodName != null) {
				return false;
			}
		} else if (!methodName.equals(other.methodName)) {
			return false;
		}
		if (signature == null) {
			if (other.signature != null) {
				return false;
			}
		} else if (!signature.equals(other.signature)) {
			return false;
		}
		return true;
	}

	public UniqueCodePoint(String className, String methodName,
			String signature, int bytecodeOffset) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.signature = signature;
		this.bytecodeOffset = bytecodeOffset;
	}
	
	public UniqueCodePoint(String className, String methodName,
			String signature) {
		this(className,methodName,signature,0);
	}

	private UniqueCodePoint(SootMethod sootMethod, int bytecodeOffset) {
		super();
		String signature = AbstractJasminClass.jasminDescriptorOf(sootMethod
				.makeRef());

		this.className = sootMethod.getDeclaringClass().getName();
		this.methodName = sootMethod.getName();
		this.signature = signature;
		this.bytecodeOffset = bytecodeOffset;
	}

	public String getClassName() {
		return className;
	}

	public UniqueCodePoint(SootMethod sootMethod) {
		this(sootMethod, 0);
	}

	public static List<UniqueCodePoint> generateUniqueCodePointsFor(
			SootMethod sootMethod, Stmt stmt) {
		// Surprisingly, a Jimple stmt can map to more than 1 bytecode offset!
		// that's why this "multi-constructor" is needed.
		// an example:
		// the Java statement: int x = d1.integer;
		// is transformed into this 2-line bytecode:
		// 1  getfield cases.lfa.Dummy.integer : int [50]
		// 4  istore_3 [x]
		// however, the Jimple equivalent is a single assignment statement.
		// so, that statement maps to both 1 and 4 bytecode offsets.

		List<UniqueCodePoint> points = new ArrayList<UniqueCodePoint>();
		List<Tag> tags = stmt.getTags();
		for (Tag tag : tags) {
			if (tag instanceof BytecodeOffsetTag) {
				int bytecodeOffset = ((BytecodeOffsetTag) tag)
						.getBytecodeOffset();
				points.add(new UniqueCodePoint(sootMethod, bytecodeOffset));
			}
		}
		return points;
	}

	public static UniqueCodePoint findFirstUniqueCodePointsFor(
			SootMethod sootMethod, Stmt stmt) {
		UniqueCodePoint point = null;
		int minBytecodeOffset = Integer.MAX_VALUE;

		for (Tag tag : stmt.getTags()) {
			if (tag instanceof BytecodeOffsetTag) {
				int bytecodeOffset = ((BytecodeOffsetTag) tag)
						.getBytecodeOffset();
				if (bytecodeOffset < minBytecodeOffset) {
					minBytecodeOffset = bytecodeOffset;
					point = new UniqueCodePoint(sootMethod, bytecodeOffset);
				}
			}
		}
		return point;
	}

	public UniqueCodePoint getJustMethod() {
		return new UniqueCodePoint(this.className, this.methodName,
				this.signature, 0);
	}
}

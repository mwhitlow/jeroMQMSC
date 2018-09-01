package com.testlims.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class StackTrace 
{	
	/** Convenience method, calls {@link StackTrace#asString(String,Throwable)}
	 *  with an empty <code>message</code> argument.
	 *  
	 *  @param e Throwable
	 *  
	 *  @return String containing printed stack trace without a message. 
	 */
	public static String asString( Throwable e) { return asString( "", e );
	}
	
	/** Like {@link #asString(Throwable)}, but prints the message argument first.
	 *  The exception message (and stack trace) associated are always printed.
	 *  
	 *  @param message Message about the throwable. 
	 *  @param e Throwable
	 *  
	 *  @return String containing the message and the printed stack trace. 
	 */
	public static String asString( String message, Throwable e) {
		StringWriter s = new StringWriter();
		PrintWriter  p = new PrintWriter(s);
		
		p.println( message );
		p.println( e.getMessage() );
		e.printStackTrace(p);
		
		return s.toString();
	}

	/** Add the stack trace (one element per line) to the indicated string buffer.
	 *  You can get the stack trace from an Exception with <code>e.getStackTrace()</code>.
	 *  
	 *  @param b add the stack trace to this string buffer.
	 *  @param stackTrace Array of Stack Trace Elements.
	 *  
	 *  @return the modified StringBuffer (the <code>b</code> argument).
	 */
	public static StringBuffer addStackTrace( StringBuffer b, StackTraceElement[] stackTrace) {
		
		b.append( '\n' );
		for( StackTraceElement element : stackTrace )
		{	b.append( element.toString() );
			b.append( '\n' );
		}
		return b;
	}

}

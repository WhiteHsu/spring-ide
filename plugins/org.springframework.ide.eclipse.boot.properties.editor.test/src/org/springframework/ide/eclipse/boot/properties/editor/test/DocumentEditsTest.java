/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.properties.editor.test;

import org.springframework.ide.eclipse.editor.support.completions.DocumentEdits;

import junit.framework.TestCase;

/**
 * @author Kris De Volder
 */
public class DocumentEditsTest extends TestCase {

	class TestSubject {
		private MockEditor editor;
		private DocumentEdits edits;
		private String orgText;

		public TestSubject(String contents) {
			this.orgText = contents;
			reset();
		}

		public void reset() {
			this.editor = new MockEditor(orgText);
			this.edits = new DocumentEdits(editor.document);
		}

		public void del(String snippet) {
			int start = orgText.indexOf(snippet);
			assertTrue(start>=0);
			int end = start + snippet.length();
			edits.delete(start, end);
		}

		public void expect(String expect) throws Exception {
			editor.apply(edits);
			assertEquals(expect, editor.getText());
		}

		public void insBefore(String before, String insert) {
			int offset = orgText.indexOf(before);
			assertTrue(offset>=0);
			edits.insert(offset, insert);
		}

		public void delLineAt(String snippet) throws Exception {
			int offset = orgText.indexOf(snippet);
			assertTrue(offset>=0);
			edits.deleteLineBackwardAtOffset(offset);
		}

		public void delLine(int i) throws Exception {
			edits.deleteLineBackward(0);
		}

	}

	public void testDeletes() throws Exception {
		TestSubject it;

		it = new TestSubject("0123456789<*>");
		it.del("123");
		it.del("567");
		it.expect("04<*>89");

		it = new TestSubject("0123456789<*>");
		it.del("567");
		it.del("123");
		it.expect("0<*>489");

		it = new TestSubject("0123456789<*>");
		it.del("012345");
		it.del("345");
		it.expect("<*>6789");

		it = new TestSubject("0123456789<*>");
		it.del("345");
		it.del("012345");
		it.expect("<*>6789");

		it = new TestSubject("0123456789<*>");
		it.del("2345");
		it.del("34567");
		it.expect("01<*>89");

		it = new TestSubject("0123456789<*>");
		it.del("123");
		it.del("234");
		it.del("7");
		it.expect("056<*>89");
	}

	public void testInserts() throws Exception {
		TestSubject it;

		it = new TestSubject("The fox jumps over the dog!");
		it.insBefore("fox", "quick ");
		it.insBefore("fox", "brown ");
		it.insBefore("dog", "lazy ");
		it.expect("The quick brown fox jumps over the lazy <*>dog!");
	}

	public void testInsertAndDelete() throws Exception {
		TestSubject it;

		it = new TestSubject("The fox jumps over the dog!");

		it.insBefore("fox", "quick ");  //"The quick fox jumps..."
		it.del("The fox");              //" jumps..."
		it.insBefore("fox", "A rabbit");//"A rabbit jumps ..."

		it.expect("A rabbit<*> jumps over the dog!");
	}

	public void testDeleteLine() throws Exception {
		TestSubject it;

		it = new TestSubject(
				"Line 0\n" +
				"Line 1\n" +
				"Line 2"
		);

		it.delLineAt("0");
		it.expect(
				"<*>Line 1\n" +
				"Line 2"
		);

		it.reset();
		it.delLineAt("1");
		it.expect(
				"Line 0<*>\n" +
				"Line 2"
		);

		it.reset();
		it.delLineAt("2");
		it.expect(
				"Line 0\n" +
				"Line 1<*>"
		);

		it = new TestSubject("Line 0"); //special case: no newlines in document
		it.delLineAt("0");
		it.expect("<*>");

		it = new TestSubject("");
		it.delLine(0);
		it.expect("<*>");

	}

}

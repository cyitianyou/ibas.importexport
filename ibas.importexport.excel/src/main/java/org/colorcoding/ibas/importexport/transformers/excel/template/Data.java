package org.colorcoding.ibas.importexport.transformers.excel.template;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板-数据区，属性值
 * 
 * @author Niuren.Zhu
 *
 */
public class Data extends Area {

	public Data() {
	}

	@Override
	public int getIndex() {
		if (this.getParent() instanceof Template) {
			Template parent = (Template) this.getParent();
			if (parent.getDatas() == this) {
				return 0;
			}
		}
		return -1;
	}

	private List<Cell[]> rows;

	public final List<Cell[]> getRows() {
		if (this.rows == null) {
			this.rows = new ArrayList<>();
		}
		return rows;
	}

	final void setRows(List<Cell[]> rows) {
		this.rows = rows;
	}

	private int columnCount;

	public int getColumnCount() {
		return this.columnCount;
	}

	final void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	public Cell[] createRow() {
		Cell[] row = new Cell[this.getColumnCount()];
		this.getRows().add(row);
		this.setEndingRow(this.getEndingRow() + 1);
		return row;
	}

}

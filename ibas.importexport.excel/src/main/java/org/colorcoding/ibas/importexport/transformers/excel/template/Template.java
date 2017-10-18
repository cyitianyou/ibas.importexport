package org.colorcoding.ibas.importexport.transformers.excel.template;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.colorcoding.ibas.bobas.bo.IBusinessObject;
import org.colorcoding.ibas.bobas.bo.IBusinessObjects;
import org.colorcoding.ibas.bobas.core.fields.IFieldData;
import org.colorcoding.ibas.bobas.core.fields.IManageFields;

/**
 * 模板（sheet）
 * 
 * @author Niuren.Zhu
 *
 */
public class Template extends Area {

	public static final String PROPERTY_PATH_SEPARATOR = ".";
	public static final String PROPERTY_PATH_LIST_SIGN = "[]";
	public static final String PROPERTY_PATH_FORMAT = "%s" + PROPERTY_PATH_SEPARATOR + "%s";
	public static final String LIST_PROPERTY_PATH_FORMAT = "%s" + PROPERTY_PATH_SEPARATOR + "%s"
			+ PROPERTY_PATH_LIST_SIGN;

	public Template() {
		this.setStartingRow(AREA_AUTO_REGION);
		this.setEndingRow(AREA_AUTO_REGION);
		this.setStartingColumn(AREA_AUTO_REGION);
		this.setEndingColumn(AREA_AUTO_REGION);
	}

	@Override
	public int getIndex() {
		return -1;
	}

	private Head head;

	/**
	 * 获取-模板头
	 * 
	 * @return
	 */
	public final Head getHead() {
		return head;
	}

	/**
	 * 设置-模板头
	 * 
	 * @param head
	 */
	private final void setHead(Head head) {
		head.setParent(this);
		this.head = head;
	}

	private List<Object> objects;

	/**
	 * 获取-模板拥有对象
	 * 
	 * @return
	 */
	public final Object[] getObjects() {
		if (this.objects == null) {
			this.objects = new ArrayList<>();
		}
		return this.objects.toArray(new Object[] {});
	}

	/**
	 * 添加-模板拥有对象
	 * 
	 * @param object
	 */
	private final void addObject(Object object) {
		if (this.objects == null) {
			this.objects = new ArrayList<>();
		}
		object.setParent(this);
		this.objects.add(object);
	}

	private Data datas;

	public final Data getDatas() {
		return datas;
	}

	private final void setDatas(Data datas) {
		datas.setParent(this);
		this.datas = datas;
	}

	@Override
	public String toString() {
		return String.format("{template: %s}", super.toString());
	}

	/**
	 * 解析对象，形成模板
	 * 
	 * 解析的对象isNew时，不处理IBOStorageTag相关属性，生成模板时应如此
	 * 
	 * @param bo
	 *            待解析对象
	 * @throws ParsingException
	 *             无法识别异常
	 */
	public final void resolving(IBusinessObject bo) throws ParsingException {
		if (bo == null) {
			// 无效数据
			return;
		}
		if (this.head == null) {
			// 未解析头
			this.resolvingHead(bo);
			this.resolvingObject(bo, this.getHead().getName());
			// 填充模板信息
			if (this.getObjects().length > 0) {
				Object lastObject = this.getObjects()[this.getObjects().length - 1];
				if (lastObject.getProperties().length > 0) {
					Property lastProperty = lastObject.getProperties()[lastObject.getProperties().length - 1];
					this.setEndingColumn(lastProperty.getEndingColumn());
					this.setEndingRow(lastProperty.getEndingRow());
					this.getHead().setEndingColumn(this.getEndingColumn());
				}
			}
			this.setName(this.getHead().getName());
			// 初始化数据区
			this.setDatas(new Data());
			this.getDatas().setColumnCount(new Function<Template, Integer>() {

				@Override
				public Integer apply(Template t) {
					int count = 0;
					for (Object object : t.getObjects()) {
						count += object.getProperties().length;
					}
					return count;
				}

			}.apply(this));
			this.getDatas().setStartingColumn(this.getStartingColumn());
			this.getDatas().setEndingColumn(this.getEndingColumn());
			this.getDatas().setStartingRow(this.getEndingRow() + 1);
		}
		if (this.getHead().getBindingClass() != bo.getClass()) {
			throw new ParsingException("data class not match template binding.");
		}
		// 解析数据
		this.resolvingDatas(bo);
	}

	/**
	 * 解析头区域
	 * 
	 * @param bo
	 * @throws ParsingException
	 */
	protected void resolvingHead(IBusinessObject bo) throws ParsingException {
		Head head = new Head();
		head.setBindingClass(bo.getClass());
		head.setName(bo.getClass().getSimpleName());
		this.setHead(head);
	}

	/**
	 * 解析对象区域
	 * 
	 * @param bo
	 * @return
	 * @throws ParsingException
	 */
	protected void resolvingObject(IBusinessObject bo, String name) throws ParsingException {
		// 根对象
		Object object = new Object();
		object.setName(name);
		object.setStartingRow(Object.OBJECT_STARTING_ROW);
		object.setEndingRow(object.getStartingRow());
		object.setStartingColumn(
				this.getObjects().length > 0 ? this.getObjects()[this.getObjects().length - 1].getEndingColumn() + 1
						: Object.OBJECT_STARTING_COLUMN);
		object.resolving(bo);
		object.setEndingColumn(object.getStartingColumn() + object.getProperties().length - 1);
		this.addObject(object);
		// 集合对象
		IManageFields fields = (IManageFields) bo;
		for (IFieldData field : fields.getFields()) {
			if (IBusinessObjects.class.isInstance(field.getValue())) {
				// 解析集合属性
				IBusinessObject subItem = ((IBusinessObjects<?, ?>) field.getValue()).create();
				if (subItem != null) {
					this.resolvingObject(subItem, String.format(LIST_PROPERTY_PATH_FORMAT, name, field.getName()));
				}
			} else if (IBusinessObject.class.isInstance(field.getValue())) {
				// 解析对象属性
				this.resolvingObject((IBusinessObject) field.getValue(),
						String.format(PROPERTY_PATH_FORMAT, name, field.getName()));
			}
		}
	}

	/**
	 * 解析数据区域
	 * 
	 * @param bo
	 * @throws ParsingException
	 */
	protected void resolvingDatas(IBusinessObject bo) throws ParsingException {
		if (bo == null || this.head == null || this.objects == null) {
			// 未初始化，退出
			return;
		}
		this.resolvingDatas((IManageFields) bo, this.getHead().getName());
	}

	private void resolvingDatas(IManageFields boFields, String level) throws ParsingException {
		if (boFields == null) {
			// 未初始化，退出
			return;
		}
		for (Object object : this.getObjects()) {
			if (!object.getName().startsWith(level)) {
				// 非此类，不做处理
				continue;
			}
			if (object.getName().equals(level)) {
				// 当前级别，同对象。如： TP - TP
				Cell[] row = this.getDatas().createRow();
				for (Property property : object.getProperties()) {
					IFieldData field = boFields.getField(property.getName());
					if (field != null && field.getValue() != null) {
						Cell cell = new Cell();
						cell.setValue(field.getValue());
						cell.setStartingColumn(property.getStartingColumn());
						cell.setEndingColumn(cell.getStartingColumn());
						cell.setStartingRow(this.getDatas().getEndingRow());
						cell.setEndingRow(cell.getStartingRow());
						row[property.getStartingColumn()] = cell;
					}
				}
			} else if (object.getName().indexOf(PROPERTY_PATH_SEPARATOR, level.length()) < 0) {
				// 当前基本，不同对象。如：TP.BB - TP or TP.AA[] - TP
				if (object.getName().endsWith(PROPERTY_PATH_LIST_SIGN)) {
					// TP.AA[] - TP
					String property = object.getName().substring(level.length(), object.getName().length() - 2);
					IFieldData field = boFields.getField(property);
					if (field != null && IBusinessObjects.class.isInstance(field.getValue())) {
						IBusinessObjects<?, ?> list = (IBusinessObjects<?, ?>) field.getValue();
						for (IBusinessObject item : list) {
							if (item instanceof IManageFields) {
								// 处理此数据
								this.resolvingDatas((IManageFields) item, object.getName());
							}
						}
					}
				} else {
					// TP.BB - TP
					String property = object.getName().substring(level.length());
					IFieldData field = boFields.getField(property);
					if (field != null && field.getValue() instanceof IManageFields) {
						this.resolvingDatas((IManageFields) field.getValue(), object.getName());
					}
				}
			}
		}
	}

	private FileWriter writer;

	public final FileWriter getWriter() {
		if (this.writer == null) {
			this.writer = new ExcelWriter();
		}
		return writer;
	}

	public final void setWriter(FileWriter writer) {
		this.writer = writer;
	}

	/**
	 * 模板内容输出文件
	 * 
	 * @param file
	 * @throws WriteFileException
	 * @throws IOException
	 */
	public void write(File file) throws WriteFileException, IOException {
		this.getWriter().setTemplate(this);
		this.getWriter().write(file);
	}

	/**
	 * 解析文件，形成模板
	 * 
	 * @param file
	 *            待分析文件
	 * @throws ParsingException
	 *             无法识别异常
	 */
	public final void resolving(File file) throws ParsingException {

	}

}

/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * JasperReports - Free Java report-generating library.
 * Copyright (C) 2001-2009 JasperSoft Corporation http://www.jaspersoft.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 *
 * JasperSoft Corporation
 * 539 Bryant Street, Suite 100
 * San Francisco, CA 94107
 * http://www.jaspersoft.com
 */

/*
 * Special thanks to Google 'Summer of Code 2005' program for supporting this development
 *
 * Contributors:
 * Majid Ali Khan - majidkk@users.sourceforge.net
 * Frank Sch�nheit - Frank.Schoenheit@Sun.COM
 */
package net.sf.jasperreports.engine.export.ooxml;

import java.awt.Color;
import java.awt.geom.Dimension2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRHyperlink;
import net.sf.jasperreports.engine.JRImage;
import net.sf.jasperreports.engine.JRImageRenderer;
import net.sf.jasperreports.engine.JRLine;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintElementIndex;
import net.sf.jasperreports.engine.JRPrintEllipse;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintHyperlink;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRPrintLine;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintRectangle;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRRenderable;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.CutsInfo;
import net.sf.jasperreports.engine.export.ExporterFilter;
import net.sf.jasperreports.engine.export.ExporterNature;
import net.sf.jasperreports.engine.export.JRExportProgressMonitor;
import net.sf.jasperreports.engine.export.JRExporterGridCell;
import net.sf.jasperreports.engine.export.JRGridLayout;
import net.sf.jasperreports.engine.export.JRHyperlinkProducer;
import net.sf.jasperreports.engine.export.ooxml.zip.FileBufferedOOXmlZip;
import net.sf.jasperreports.engine.export.ooxml.zip.OOXmlZip;
import net.sf.jasperreports.engine.export.ooxml.zip.OOXmlZipEntry;
import net.sf.jasperreports.engine.util.JRProperties;
import net.sf.jasperreports.engine.util.JRStringUtil;
import net.sf.jasperreports.engine.util.JRStyledText;


/**
 * Exports a JasperReports document to Open Office XML format. It has character output type and exports the document to a
 * grid-based layout.
 * @author sanda zaharia (shertage@users.sourceforge.net)
 * @version $Id$
 */
public abstract class JROpenOfficeXmlExporter extends JRAbstractExporter
{

	protected static final String DOCX_EXPORTER_PROPERTIES_PREFIX = JRProperties.PROPERTY_PREFIX + "export.docx.";
	protected static final String XLSX_EXPORTER_PROPERTIES_PREFIX = JRProperties.PROPERTY_PREFIX + "export.xlsx.";
	protected static final String UTF8_ENCODING = "UTF-8";	

	/**
	 *
	 */
	protected static final String JR_PAGE_ANCHOR_PREFIX = "JR_PAGE_ANCHOR_";

	/**
	 *
	 */
	protected static final String HORIZONTAL_ALIGN_LEFT = "start";
	protected static final String HORIZONTAL_ALIGN_RIGHT = "end";
	protected static final String HORIZONTAL_ALIGN_CENTER = "center";
	protected static final String HORIZONTAL_ALIGN_JUSTIFY = "justified";

	/**
	 *
	 */
	protected static final String VERTICAL_ALIGN_TOP = "top";
	protected static final String VERTICAL_ALIGN_MIDDLE = "middle";
	protected static final String VERTICAL_ALIGN_BOTTOM = "bottom";

	public static final String IMAGE_NAME_PREFIX = "img_";
	protected static final int IMAGE_NAME_PREFIX_LEGTH = IMAGE_NAME_PREFIX.length();

	/**
	 *
	 */
	protected Writer tempDocWriter = null;
	protected Writer tempStyleWriter = null;
	protected Writer tempFontWriter = null;

	protected JRExportProgressMonitor progressMonitor = null;
	protected Map rendererToImagePathMap = null;
	protected Map imageMaps;
	protected List imagesToProcess = null;

	protected int reportIndex = 0;
	protected int pageIndex = 0;
	protected int tableIndex = 0;
	protected boolean startPage;

	/**
	 *
	 */
	protected String encoding = null;


	/**
	 *
	 */
	protected boolean isWrapBreakWord = false;

	protected Map fontMap = null;

	protected LinkedList backcolorStack;
	protected Color backcolor;

	private StyleCache styleCache = null;

	protected ExporterNature nature = null;
	protected String exporterPropertiesPrefix;

	public JROpenOfficeXmlExporter()
	{
		backcolorStack = new LinkedList();
		backcolor = null;
	}


	/**
	 *
	 */
	public void exportReport() throws JRException
	{
		progressMonitor = (JRExportProgressMonitor)parameters.get(JRExporterParameter.PROGRESS_MONITOR);

		/*   */
		setOffset();

		try
		{
			/*   */
			setExportContext();

			/*   */
			setInput();

			if (!parameters.containsKey(JRExporterParameter.FILTER))
			{
				filter = createFilter(exporterPropertiesPrefix);
			}

			/*   */
			if (!isModeBatch)
			{
				setPageRange();
			}

			encoding =
				getStringParameterOrDefault(
					JRExporterParameter.CHARACTER_ENCODING,
					JRExporterParameter.PROPERTY_CHARACTER_ENCODING
					);

			rendererToImagePathMap = new HashMap();
			imageMaps = new HashMap();
			imagesToProcess = new ArrayList();

			fontMap = (Map) parameters.get(JRExporterParameter.FONT_MAP);

			setHyperlinkProducerFactory();

			nature = getExporterNature(filter);

			OutputStream os = (OutputStream)parameters.get(JRExporterParameter.OUTPUT_STREAM);
			if (os != null)
			{
				try
				{
					exportReportToOOXmlZip(os);
				}
				catch (IOException e)
				{
					throw new JRException("Error trying to export to output stream : " + jasperPrint.getName(), e);
				}
			}
			else
			{
				File destFile = (File)parameters.get(JRExporterParameter.OUTPUT_FILE);
				if (destFile == null)
				{
					String fileName = (String)parameters.get(JRExporterParameter.OUTPUT_FILE_NAME);
					if (fileName != null)
					{
						destFile = new File(fileName);
					}
					else
					{
						throw new JRException("No output specified for the exporter.");
					}
				}

				try
				{
					os = new FileOutputStream(destFile);
					exportReportToOOXmlZip(os);
				}
				catch (IOException e)
				{
					throw new JRException("Error trying to export to file : " + destFile, e);
				}
				finally
				{
					if (os != null)
					{
						try
						{
							os.close();
						}
						catch(IOException e)
						{
						}
					}
				}
			}
		}
		finally
		{
			resetExportContext();
		}
	}


	/**
	 *
	 */
	public static JRPrintImage getImage(List jasperPrintList, String imageName)
	{
		return getImage(jasperPrintList, getPrintElementIndex(imageName));
	}


	public static JRPrintImage getImage(List jasperPrintList, JRPrintElementIndex imageIndex)
	{
		JasperPrint report = (JasperPrint)jasperPrintList.get(imageIndex.getReportIndex());
		JRPrintPage page = (JRPrintPage)report.getPages().get(imageIndex.getPageIndex());

		Integer[] elementIndexes = imageIndex.getAddressArray();
		Object element = page.getElements().get(elementIndexes[0].intValue());

		for (int i = 1; i < elementIndexes.length; ++i)
		{
			JRPrintFrame frame = (JRPrintFrame) element;
			element = frame.getElements().get(elementIndexes[i].intValue());
		}

		return (JRPrintImage) element;
	}


	/**
	 *
	 */
	protected void exportReportToOOXmlZip(OutputStream os) throws JRException, IOException
	{

		String xmlProlog = encoding == null ? 
				"<?xml version=\"1.0\" encoding=\"" + UTF8_ENCODING + "\" standalone=\"yes\"?> \r\n" :
				"<?xml version=\"1.0\" encoding=\"" + encoding + "\" standalone=\"yes\"?> \r\n";

//		StyleBuilder styleBuilder = new StyleBuilder(jasperPrintList, tempStyleWriter, xmlProlog);
//		styleBuilder.build();
		StyleBuilder styleBuilder = null;
		
		for(reportIndex = 0; reportIndex < jasperPrintList.size(); reportIndex++)
		{
			
			setJasperPrint((JasperPrint)jasperPrintList.get(reportIndex));
			
			OOXmlZip ooxmlZip = new FileBufferedOOXmlZip(
					((JROpenOfficeXmlExporterNature)nature).getOpenOfficeXmlNature(),
					encoding
					);

			OOXmlZipEntry tempDocEntry = ooxmlZip.getDocumentEntry();
			OOXmlZipEntry tempStyleEntry = ooxmlZip.getStylesEntry();

			tempDocWriter = tempDocEntry.getWriter();
			tempStyleWriter = tempStyleEntry.getWriter();

			styleCache = new StyleCache(tempStyleWriter, tempDocWriter, fontMap);

			styleBuilder = new StyleBuilder(tempStyleWriter, xmlProlog);
			styleBuilder.build();
			
			writeDocHeader(xmlProlog);
			List pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0)
			{
				if (isModeBatch)
				{
					startPageIndex = 0;
					endPageIndex = pages.size() - 1;
				}

				JRPrintPage page = null;
				for(pageIndex = startPageIndex; pageIndex <= endPageIndex; pageIndex++)
				{
					if (Thread.currentThread().isInterrupted())
					{
						throw new JRException("Current thread interrupted.");
					}

					page = (JRPrintPage)pages.get(pageIndex);

					exportPage(page);
				}
			}
			styleBuilder.buildStylesFooter();
			writeDocFooter(jasperPrint.getPageWidth(), 
					jasperPrint.getPageHeight());
			
			tempDocWriter.flush();
			tempStyleWriter.flush();

			tempDocWriter.close();
			tempStyleWriter.close();
			
			ooxmlZip.zipEntries(os);
			ooxmlZip.dispose();
		}



		/*   */
//		ContentBuilder contentBuilder =
//			new ContentBuilder(
//				ooxmlZip.getContentTypesEntry(),
//				tempStyleEntry,
//				tempBodyEntry,
//				styleCache.getFontFaces(),
//				((JROpenOfficeXmlExporterNature)nature).getOpenOfficeXmlNature()
//				);
//		contentBuilder.build();
//
//		tempStyleEntry.dispose();
//		tempBodyEntry.dispose();
//
//		if ((imagesToProcess != null && imagesToProcess.size() > 0))
//		{
//			for(Iterator it = imagesToProcess.iterator(); it.hasNext();)
//			{
//				JRPrintElementIndex imageIndex = (JRPrintElementIndex)it.next();
//
//				JRPrintImage image = getImage(jasperPrintList, imageIndex);
//				JRRenderable renderer = image.getRenderer();
//				if (renderer.getType() == JRRenderable.TYPE_SVG)
//				{
//					renderer =
//						new JRWrappingSvgRenderer(
//							renderer,
//							new Dimension(image.getWidth(), image.getHeight()),
//							JRElement.MODE_OPAQUE == image.getMode() ? image.getBackcolor() : null
//							);
//				}
//
//				ooxmlZip.addEntry(//FIXMEODT optimize with a different implementation of entry
//					new FileBufferedOOXmlZipEntry(
//						"Pictures/" + getImageName(imageIndex),
//						renderer.getImageData()
//						)
//					);
//			}
//		}

	}


	/**
	 *
	 */
	protected void exportPage(JRPrintPage page) throws JRException, IOException
	{
		startPage = true;
		JRGridLayout layout =
			new JRGridLayout(
				nature,
				page.getElements(),
				jasperPrint.getPageWidth(),
				jasperPrint.getPageHeight(),
				globalOffsetX,
				globalOffsetY,
				null //address
				);

		exportGrid(layout, null);

		if (progressMonitor != null)
		{
			progressMonitor.afterPageExport();
		}
	}


	/**
	 *
	 */
	protected void exportGrid(JRGridLayout gridLayout, JRPrintElementIndex frameIndex) throws IOException, JRException
	{
		CutsInfo xCuts = gridLayout.getXCuts();
		JRExporterGridCell[][] grid = gridLayout.getGrid();

		TableBuilder tableBuilder = frameIndex == null
			? new TableBuilder(reportIndex, pageIndex, tempDocWriter, tempStyleWriter)
			: new TableBuilder(frameIndex.toString(), tempDocWriter, tempStyleWriter);

//		tableBuilder.buildTableStyle();
		tableBuilder.buildTableHeader(xCuts.size());

//		for(int col = 1; col < xCuts.size(); col++)
//		{
//			tableBuilder.buildColumnStyle(
//					col - 1,
//					xCuts.getCut(col) - xCuts.getCut(col - 1)
//					);
//			tableBuilder.buildColumnHeader(col - 1);
//			tableBuilder.buildColumnFooter();
//		}

		JRPrintElement element = null;
		for(int row = 0; row < grid.length; row++)
		{
			int rowHeight = gridLayout.getRowHeight(row);
			if(rowHeight == 0)
			{
				continue;
			}
			int emptyCellColSpan = 0;
			int emptyCellWidth = 0;

//			tableBuilder.buildRowStyle(row, rowHeight);
			tableBuilder.buildRowHeader(row);

			for(int col = 0; col < grid[0].length; col++)
			{
				JRExporterGridCell gridCell = grid[row][col];
				if(gridCell.getWidth() == 0)
				{
					continue;
				}
				
				if (gridCell == JRExporterGridCell.OCCUPIED_CELL)
				{
					if (emptyCellColSpan > 0)
					{
						writeEmptyCell(gridCell, emptyCellColSpan, emptyCellWidth, rowHeight);
						emptyCellColSpan = 0;
						emptyCellWidth = 0;
					}

//					writeOccupiedCells(1);
				}
				else if(gridCell.getWrapper() != null)
				{
					int colspan = 0;
					element = gridCell.getWrapper().getElement();
					if (emptyCellColSpan > 0)
					{
//						writeCellHeader(gridCell, emptyCellColSpan, emptyCellWidth, rowHeight);
						tableBuilder.buildCellHeader(styleCache.getCellStyle(element), emptyCellColSpan, gridCell.getRowSpan());
						
						emptyCellColSpan = 0;
						emptyCellWidth = 0;
					}
					else
					{
						tableBuilder.buildCellHeader(styleCache.getCellStyle(element), gridCell.getColSpan(), gridCell.getRowSpan());
					}
					

					if (element instanceof JRPrintLine)
					{
//						exportLine(tableBuilder, (JRPrintLine)element, gridCell);
					}
					else if (element instanceof JRPrintRectangle)
					{
//						exportRectangle(tableBuilder, (JRPrintRectangle)element, gridCell);
					}
					else if (element instanceof JRPrintEllipse)
					{
//						exportEllipse(tableBuilder, (JRPrintEllipse)element, gridCell);
					}
					else if (element instanceof JRPrintImage)
					{
//						exportImage(tableBuilder, (JRPrintImage)element, gridCell);
					}
					else if (element instanceof JRPrintText)
					{
						exportText(tableBuilder, (JRPrintText)element, gridCell);
					}
					else if (element instanceof JRPrintFrame)
					{
//						exportFrame(tableBuilder, (JRPrintFrame)element, gridCell);
					}
					tableBuilder.buildCellFooter();
					
					//x += gridCell.colSpan - 1;
				}
				else
				{
					emptyCellColSpan++;
					emptyCellWidth += gridCell.getWidth();
				}
				
			}

			if (emptyCellColSpan > 0)
			{
				writeEmptyCell(null, emptyCellColSpan, emptyCellWidth, rowHeight);
			}

			tableBuilder.buildRowFooter();
		}

		tableBuilder.buildTableFooter();
	}


	private void writeEmptyCell(JRExporterGridCell gridCell, int emptyCellColSpan, int emptyCellWidth, int rowHeight) throws IOException
	{
		tempDocWriter.write("    <w:tc> \r\n");
		tempDocWriter.write("     <w:tcPr> \r\n");
		tempDocWriter.write("      <w:tcW w:w=\"" + Utility.translatePointsToTwips(emptyCellWidth) + "\" w:type=\"dxa\" /> \r\n");
		if (emptyCellColSpan > 1)
		{
			tempDocWriter.write("      <w:gridSpan w:val=\"" + emptyCellColSpan +"\" /> \r\n");
		}
		tempDocWriter.write("     </w:tcPr> \r\n");
		tempDocWriter.write("     <w:p/> \r\n");
		tempDocWriter.write("    </w:tc> \r\n");
		
		tempDocWriter.flush();

//		writeOccupiedCells(emptyCellColSpan - 1);
	}
	
	private void writeCellFooter() throws IOException
	{
		tempDocWriter.write("    </w:tc> \r\n");
		tempDocWriter.flush();
	}


//	private void writeOccupiedCells(int count) throws IOException
//	{
//		for(int i = 0; i < count; i++)
//		{
//			tempDocWriter.write("<table:covered-table-cell/>\n");
//		}
//	}


	/**
	 *
	 */
	protected void exportLine(TableBuilder tableBuilder, JRPrintLine line, JRExporterGridCell gridCell) throws IOException
	{
		tableBuilder.buildCellHeader(null, gridCell.getColSpan(), gridCell.getRowSpan());

		double x1, y1, x2, y2;

		if (line.getDirection() == JRLine.DIRECTION_TOP_DOWN)
		{
			x1 = Utility.translatePixelsToInches(0);
			y1 = Utility.translatePixelsToInches(0);
			x2 = Utility.translatePixelsToInches(line.getWidth() - 1);
			y2 = Utility.translatePixelsToInches(line.getHeight() - 1);
		}
		else
		{
			x1 = Utility.translatePixelsToInches(0);
			y1 = Utility.translatePixelsToInches(line.getHeight() - 1);
			x2 = Utility.translatePixelsToInches(line.getWidth() - 1);
			y2 = Utility.translatePixelsToInches(0);
		}

		tempDocWriter.write("<text:p>");
		insertPageAnchor();
		tempDocWriter.write(
				"<draw:line text:anchor-type=\"paragraph\" "
				+ "draw:style-name=\"" + styleCache.getGraphicStyle(line) + "\" "
				+ "svg:x1=\"" + x1 + "in\" "
				+ "svg:y1=\"" + y1 + "in\" "
				+ "svg:x2=\"" + x2 + "in\" "
				+ "svg:y2=\"" + y2 + "in\">"
				//+ "</draw:line>"
				+ "<text:p/></draw:line>"
				+ "</text:p>"
				);
		tableBuilder.buildCellFooter();
	}


	/**
	 *
	 */
	protected void exportRectangle(TableBuilder tableBuilder, JRPrintRectangle rectangle, JRExporterGridCell gridCell) throws IOException
	{
		tableBuilder.buildCellHeader(styleCache.getCellStyle(rectangle), gridCell.getColSpan(), gridCell.getRowSpan());
		tableBuilder.buildCellFooter();
	}


	/**
	 *
	 */
	protected void exportEllipse(TableBuilder tableBuilder, JRPrintEllipse ellipse, JRExporterGridCell gridCell) throws IOException
	{
		tableBuilder.buildCellHeader(null, gridCell.getColSpan(), gridCell.getRowSpan());
		tempDocWriter.write("<text:p>");
		insertPageAnchor();
		tempDocWriter.write(
			"<draw:ellipse text:anchor-type=\"paragraph\" "
			+ "draw:style-name=\"" + styleCache.getGraphicStyle(ellipse) + "\" "
			+ "svg:width=\"" + Utility.translatePixelsToInches(ellipse.getWidth()) + "in\" "
			+ "svg:height=\"" + Utility.translatePixelsToInches(ellipse.getHeight()) + "in\" "
			+ "svg:x=\"0in\" "
			+ "svg:y=\"0in\">"
			+ "<text:p/></draw:ellipse></text:p>"
			);
		tableBuilder.buildCellFooter();
	}


	/**
	 *
	 */
	protected void exportText(TableBuilder tableBuilder, JRPrintText text, JRExporterGridCell gridCell) throws IOException
	{

		JRStyledText styledText = getStyledText(text);

		int textLength = 0;

		if (styledText != null)
		{
			textLength = styledText.length();
		}

//		if (isWrapBreakWord)
//		{
//			styleBuffer.append("width: " + gridCell.width + "; ");
//			styleBuffer.append("word-wrap: break-word; ");
//		}

//		if (text.getLineSpacing() != JRTextElement.LINE_SPACING_SINGLE)
//		{
//			styleBuffer.append("line-height: " + text.getLineSpacingFactor() + "; ");
//		}

//		if (styleBuffer.length() > 0)
//		{
//			writer.write(" style=\"");
//			writer.write(styleBuffer.toString());
//			writer.write("\"");
//		}
//
//		writer.write(">");
		tempDocWriter.write("     <w:p> \r\n");
		tempDocWriter.write("      <w:pPr> \r\n");
		tempDocWriter.write("        <w:pStyle w:val=\"" + styleCache.getParagraphStyle(text) + "\"/> \r\n");
		tempDocWriter.write("      </w:pPr> \r\n");
		tempDocWriter.flush();
		
//		insertPageAnchor();
//		if (text.getAnchorName() != null)
//		{
//			tempDocWriter.write("<text:bookmark text:name=\"");
//			tempDocWriter.write(text.getAnchorName());
//			tempDocWriter.write("\"/>");
//		}
//
//		boolean startedHyperlink = startHyperlink(text, true);

		if (textLength > 0)
		{
			exportStyledText(styledText);
		}

//		if (startedHyperlink)
//		{
//			endHyperlink(true);
//		}

		tempDocWriter.write("     </w:p> \r\n");
		tempDocWriter.flush();
	}


	/**
	 *
	 */
	protected void exportStyledText(JRStyledText styledText) throws IOException
	{
		String text = styledText.getText();

		int runLimit = 0;

		AttributedCharacterIterator iterator = styledText.getAttributedString().getIterator();

		while(runLimit < styledText.length() && (runLimit = iterator.getRunLimit()) <= styledText.length())
		{
			exportStyledTextRun(iterator.getAttributes(), text.substring(iterator.getIndex(), runLimit));

			iterator.setIndex(runLimit);
		}
	}


	/**
	 *
	 */
	protected void exportStyledTextRun(Map attributes, String text) throws IOException
	{
		tempDocWriter.write("      <w:r> \r\n");
		tempDocWriter.write("       <w:rPr> \r\n");
		styleCache.writeTextSpanStyle(attributes, text);
		tempDocWriter.write("       </w:rPr> \r\n");
		tempDocWriter.write("       <w:t> \r\n");
		if (text != null)
		{
			tempDocWriter.write(JRStringUtil.xmlEncode(text) + " \r\n");//FIXMEODT try something nicer for replace
		}
		tempDocWriter.write("       </w:t> \r\n");
		tempDocWriter.write("      </w:r> \r\n");
		tempDocWriter.flush();
	}

	/**
	 *
	 */
	protected void exportImage(TableBuilder tableBuilder, JRPrintImage image, JRExporterGridCell gridCell) throws JRException, IOException
	{
		int topPadding = 
			Math.max(image.getLineBox().getTopPadding().intValue(), Math.round(image.getLineBox().getTopPen().getLineWidth().floatValue()));
		int leftPadding = 
			Math.max(image.getLineBox().getLeftPadding().intValue(), Math.round(image.getLineBox().getLeftPen().getLineWidth().floatValue()));
		int bottomPadding = 
			Math.max(image.getLineBox().getBottomPadding().intValue(), Math.round(image.getLineBox().getBottomPen().getLineWidth().floatValue()));
		int rightPadding = 
			Math.max(image.getLineBox().getRightPadding().intValue(), Math.round(image.getLineBox().getRightPen().getLineWidth().floatValue()));

		int availableImageWidth = image.getWidth() - leftPadding - rightPadding;
		availableImageWidth = availableImageWidth < 0 ? 0 : availableImageWidth;

		int availableImageHeight = image.getHeight() - topPadding - bottomPadding;
		availableImageHeight = availableImageHeight < 0 ? 0 : availableImageHeight;

		int width = availableImageWidth;
		int height = availableImageHeight;

		int xoffset = 0;
		int yoffset = 0;

		tableBuilder.buildCellHeader(styleCache.getCellStyle(image), gridCell.getColSpan(), gridCell.getRowSpan());

		JRRenderable renderer = image.getRenderer();

		if (
			renderer != null &&
			availableImageWidth > 0 &&
			availableImageHeight > 0
			)
		{
			if (renderer.getType() == JRRenderable.TYPE_IMAGE && !image.isLazy())
			{
				// Non-lazy image renderers are all asked for their image data at some point.
				// Better to test and replace the renderer now, in case of lazy load error.
				renderer = JRImageRenderer.getOnErrorRendererForImageData(renderer, image.getOnErrorType());
			}
		}
		else
		{
			renderer = null;
		}

		if (renderer != null)
		{
			float xalignFactor = getXAlignFactor(image);
			float yalignFactor = getYAlignFactor(image);

			switch (image.getScaleImage())
			{
				case JRImage.SCALE_IMAGE_FILL_FRAME :
				{
					width = availableImageWidth;
					height = availableImageHeight;
					xoffset = 0;
					yoffset = 0;
					break;
				}
				case JRImage.SCALE_IMAGE_CLIP :
				case JRImage.SCALE_IMAGE_RETAIN_SHAPE :
				default :
				{
					double normalWidth = availableImageWidth;
					double normalHeight = availableImageHeight;

					if (!image.isLazy())
					{
						// Image load might fail.
						JRRenderable tmpRenderer =
							JRImageRenderer.getOnErrorRendererForDimension(renderer, image.getOnErrorType());
						Dimension2D dimension = tmpRenderer == null ? null : tmpRenderer.getDimension();
						// If renderer was replaced, ignore image dimension.
						if (tmpRenderer == renderer && dimension != null)
						{
							normalWidth = dimension.getWidth();
							normalHeight = dimension.getHeight();
						}
					}

					if (availableImageHeight > 0)
					{
						double ratio = (double)normalWidth / (double)normalHeight;

						if( ratio > availableImageWidth / (double)availableImageHeight )
						{
							width = availableImageWidth;
							height = (int)(width/ratio);

						}
						else
						{
							height = availableImageHeight;
							width = (int)(ratio * height);
						}
					}

					xoffset = (int)(xalignFactor * (availableImageWidth - width));
					yoffset = (int)(yalignFactor * (availableImageHeight - height));
				}
			}

			tempDocWriter.write("<text:p>");
			insertPageAnchor();
			if (image.getAnchorName() != null)
			{
				tempDocWriter.write("<text:bookmark text:name=\"");
				tempDocWriter.write(image.getAnchorName());
				tempDocWriter.write("\"/>");
			}


			boolean startedHyperlink = startHyperlink(image,false);

			tempDocWriter.write("<draw:frame text:anchor-type=\"paragraph\" "
					+ "draw:style-name=\"" + styleCache.getGraphicStyle(image) + "\" "
					+ "svg:x=\"" + Utility.translatePixelsToInches(leftPadding + xoffset) + "in\" "
					+ "svg:y=\"" + Utility.translatePixelsToInches(topPadding + yoffset) + "in\" "
					+ "svg:width=\"" + Utility.translatePixelsToInches(width) + "in\" "
					+ "svg:height=\"" + Utility.translatePixelsToInches(height) + "in\">"
					);
			tempDocWriter.write("<draw:image ");
			tempDocWriter.write(" xlink:href=\"" + getImagePath(renderer, image.isLazy(), gridCell) + "\"");
			tempDocWriter.write(" xlink:type=\"simple\"");
			tempDocWriter.write(" xlink:show=\"embed\"");
			tempDocWriter.write(" xlink:actuate=\"onLoad\"");
			tempDocWriter.write("/>\n");

			tempDocWriter.write("</draw:frame>");
			if(startedHyperlink)
			{
				endHyperlink(false);
			}
			tempDocWriter.write("</text:p>");
		}

		tableBuilder.buildCellFooter();
	}


	/**
	 *
	 */
	protected String getImagePath(JRRenderable renderer, boolean isLazy, JRExporterGridCell gridCell) throws IOException
	{
		String imagePath = null;

		if (renderer != null)
		{
			if (renderer.getType() == JRRenderable.TYPE_IMAGE && rendererToImagePathMap.containsKey(renderer.getId()))
			{
				imagePath = (String)rendererToImagePathMap.get(renderer.getId());
			}
			else
			{
				if (isLazy)
				{
					imagePath = ((JRImageRenderer)renderer).getImageLocation();
				}
				else
				{
					JRPrintElementIndex imageIndex = getElementIndex(gridCell);
					imagesToProcess.add(imageIndex);

					String imageName = getImageName(imageIndex);
					imagePath = "Pictures/" + imageName;
				}

				rendererToImagePathMap.put(renderer.getId(), imagePath);
			}
		}

		return imagePath;
	}


	protected JRPrintElementIndex getElementIndex(JRExporterGridCell gridCell)
	{
		JRPrintElementIndex imageIndex =
			new JRPrintElementIndex(
					reportIndex,
					pageIndex,
					gridCell.getWrapper().getAddress()
					);
		return imageIndex;
	}


	/**
	 *
	 *
	protected void writeImageMap(String imageMapName, JRPrintHyperlink mainHyperlink, List imageMapAreas) throws IOException
	{
		writer.write("<map name=\"" + imageMapName + "\">\n");

		for (Iterator it = imageMapAreas.iterator(); it.hasNext();)
		{
			JRPrintImageAreaHyperlink areaHyperlink = (JRPrintImageAreaHyperlink) it.next();
			JRPrintImageArea area = areaHyperlink.getArea();

			writer.write("  <area shape=\"" + JRPrintImageArea.getHtmlShape(area.getShape()) + "\"");
			writeImageAreaCoordinates(area);
			writeImageAreaHyperlink(areaHyperlink.getHyperlink());
			writer.write("/>\n");
		}

		if (mainHyperlink.getHyperlinkType() != JRHyperlink.HYPERLINK_TYPE_NONE)
		{
			writer.write("  <area shape=\"default\"");
			writeImageAreaHyperlink(mainHyperlink);
			writer.write("/>\n");
		}

		writer.write("</map>\n");
	}


	protected void writeImageAreaCoordinates(JRPrintImageArea area) throws IOException
	{
		int[] coords = area.getCoordinates();
		if (coords != null && coords.length > 0)
		{
			StringBuffer coordsEnum = new StringBuffer(coords.length * 4);
			coordsEnum.append(coords[0]);
			for (int i = 1; i < coords.length; i++)
			{
				coordsEnum.append(',');
				coordsEnum.append(coords[i]);
			}

			writer.write(" coords=\"" + coordsEnum + "\"");
		}
	}


	protected void writeImageAreaHyperlink(JRPrintHyperlink hyperlink) throws IOException
	{
		String href = getHyperlinkURL(hyperlink);
		if (href == null)
		{
			writer.write(" nohref=\"nohref\"");
		}
		else
		{
			writer.write(" href=\"" + href + "\"");

			String target = getHyperlinkTarget(hyperlink);
			if (target != null)
			{
				writer.write(" target=\"");
				writer.write(target);
				writer.write("\"");
			}
		}

		if (hyperlink.getHyperlinkTooltip() != null)
		{
			writer.write(" title=\"");
			writer.write(JRStringUtil.xmlEncode(hyperlink.getHyperlinkTooltip()));
			writer.write("\"");
		}
	}


	/**
	 *
	 */
	public static String getImageName(JRPrintElementIndex printElementIndex)
	{
		return IMAGE_NAME_PREFIX + printElementIndex.toString();
	}


	/**
	 *
	 */
	public static JRPrintElementIndex getPrintElementIndex(String imageName)
	{
		if (!imageName.startsWith(IMAGE_NAME_PREFIX))
		{
			throw new JRRuntimeException("Invalid image name: " + imageName);
		}

		return JRPrintElementIndex.parsePrintElementIndex(imageName.substring(IMAGE_NAME_PREFIX_LEGTH));
	}


	/**
	 *
	 */
	protected void exportFrame(TableBuilder tableBuilder, JRPrintFrame frame, JRExporterGridCell gridCell) throws IOException, JRException
	{
		tableBuilder.buildCellHeader(styleCache.getCellStyle(frame), gridCell.getColSpan(), gridCell.getRowSpan());

		boolean appendBackcolor =
			frame.getMode() == JRElement.MODE_OPAQUE
			&& (backcolor == null || frame.getBackcolor().getRGB() != backcolor.getRGB());

		if (appendBackcolor)
		{
			setBackcolor(frame.getBackcolor());
		}

		try
		{
			JRGridLayout layout = gridCell.getLayout();
			JRPrintElementIndex frameIndex =
				new JRPrintElementIndex(
						reportIndex,
						pageIndex,
						gridCell.getWrapper().getAddress()
						);
			exportGrid(layout, frameIndex);
		}
		finally
		{
			if (appendBackcolor)
			{
				restoreBackcolor();
			}
		}

		tableBuilder.buildCellFooter();
	}

	/**
	 *
	 */
	protected void setBackcolor(Color color)
	{
		backcolorStack.addLast(backcolor);

		backcolor = color;
	}


	protected void restoreBackcolor()
	{
		backcolor = (Color) backcolorStack.removeLast();
	}


	private float getXAlignFactor(JRPrintImage image)
	{
		float xalignFactor = 0f;
		switch (image.getHorizontalAlignment())
		{
			case JRAlignment.HORIZONTAL_ALIGN_RIGHT :
			{
				xalignFactor = 1f;
				break;
			}
			case JRAlignment.HORIZONTAL_ALIGN_CENTER :
			{
				xalignFactor = 0.5f;
				break;
			}
			case JRAlignment.HORIZONTAL_ALIGN_LEFT :
			default :
			{
				xalignFactor = 0f;
				break;
			}
		}
		return xalignFactor;
	}


	private float getYAlignFactor(JRPrintImage image)
	{
		float yalignFactor = 0f;
		switch (image.getVerticalAlignment())
		{
			case JRAlignment.VERTICAL_ALIGN_BOTTOM :
			{
				yalignFactor = 1f;
				break;
			}
			case JRAlignment.VERTICAL_ALIGN_MIDDLE :
			{
				yalignFactor = 0.5f;
				break;
			}
			case JRAlignment.VERTICAL_ALIGN_TOP :
			default :
			{
				yalignFactor = 0f;
				break;
			}
		}
		return yalignFactor;
	}

	protected boolean startHyperlink(JRPrintHyperlink link, boolean isText) throws IOException
	{
		String href = getHyperlinkURL(link);

		if (href != null)
		{
			if(isText)
			{
				tempDocWriter.write("<text:a xlink:href=\"");
			}
			else
			{
				tempDocWriter.write("<draw:a xlink:type=\"simple\" xlink:href=\"");
			}
			tempDocWriter.write(href);
			tempDocWriter.write("\"");


			String target = getHyperlinkTarget(link);//FIXMETARGET
			if (target != null)
			{
				tempDocWriter.write(" office:target-frame-name=\"");
				tempDocWriter.write(target);
				tempDocWriter.write("\"");
				if(target.equals("_blank"))
				{
					tempDocWriter.write(" xlink:show=\"new\"");
				}
			}
/*
 * tooltips are unavailable for the moment
 *
			if (link.getHyperlinkTooltip() != null)
			{
				tempDocWriter.write(" xlink:title=\"");
				tempDocWriter.write(JRStringUtil.xmlEncode(link.getHyperlinkTooltip()));
				tempDocWriter.write("\"");
			}
*/
			tempDocWriter.write(">");
		}

		return href != null;
	}


	protected String getHyperlinkTarget(JRPrintHyperlink link)
	{
		String target = null;
		switch(link.getHyperlinkTarget())
		{
			case JRHyperlink.HYPERLINK_TARGET_SELF :
			{
				target = "_self";
				break;
			}
			case JRHyperlink.HYPERLINK_TARGET_BLANK :
			default :
			{
				target = "_blank";
				break;
			}
		}
		return target;
	}


	protected String getHyperlinkURL(JRPrintHyperlink link)
	{
		String href = null;
		JRHyperlinkProducer customHandler = getCustomHandler(link);
		if (customHandler == null)
		{
			switch(link.getHyperlinkType())
			{
				case JRHyperlink.HYPERLINK_TYPE_REFERENCE :
				{
					if (link.getHyperlinkReference() != null)
					{
						href = link.getHyperlinkReference();
					}
					break;
				}
				case JRHyperlink.HYPERLINK_TYPE_LOCAL_ANCHOR :
				{
					if (link.getHyperlinkAnchor() != null)
					{
						href = "#" + link.getHyperlinkAnchor();
					}
					break;
				}
				case JRHyperlink.HYPERLINK_TYPE_LOCAL_PAGE :
				{
					if (link.getHyperlinkPage() != null)
					{
						href = "#" + JR_PAGE_ANCHOR_PREFIX + reportIndex + "_" + link.getHyperlinkPage().toString();
					}
					break;
				}
				case JRHyperlink.HYPERLINK_TYPE_REMOTE_ANCHOR :
				{
					if (
						link.getHyperlinkReference() != null &&
						link.getHyperlinkAnchor() != null
						)
					{
						href = link.getHyperlinkReference() + "#" + link.getHyperlinkAnchor();
					}
					break;
				}
				case JRHyperlink.HYPERLINK_TYPE_REMOTE_PAGE :
				{
					if (
						link.getHyperlinkReference() != null &&
						link.getHyperlinkPage() != null
						)
					{
						href = link.getHyperlinkReference() + "#" + JR_PAGE_ANCHOR_PREFIX + "0_" + link.getHyperlinkPage().toString();
					}
					break;
				}
				case JRHyperlink.HYPERLINK_TYPE_NONE :
				default :
				{
					break;
				}
			}
		}
		else
		{
			href = customHandler.getHyperlink(link);
		}

		return href;
	}


	protected void endHyperlink(boolean isText) throws IOException
	{
		if(isText)
			tempDocWriter.write("</text:a>");
		else
			tempDocWriter.write("</draw:a>");
	}

	protected void insertPageAnchor() throws IOException
	{
		if(startPage)
		{
			tempDocWriter.write("<text:bookmark text:name=\"");
			tempDocWriter.write(JR_PAGE_ANCHOR_PREFIX + reportIndex + "_" + (pageIndex + 1));
			tempDocWriter.write("\"/>\n");
			startPage = false;
		}
	}

	protected void writeDocHeader(String xmlProlog) throws IOException
	{
		tempDocWriter.write(xmlProlog + " \r\n");
		tempDocWriter.write("<w:document \r\n");
		tempDocWriter.write(" xmlns:ve=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" \r\n");
		tempDocWriter.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\" \r\n");
		tempDocWriter.write(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" \r\n");
		tempDocWriter.write(" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\" \r\n");
		tempDocWriter.write(" xmlns:v=\"urn:schemas-microsoft-com:vml\" \r\n");
		tempDocWriter.write(" xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" \r\n");
		tempDocWriter.write(" xmlns:w10=\"urn:schemas-microsoft-com:office:word\" \r\n");
		tempDocWriter.write(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" \r\n");
		tempDocWriter.write(" xmlns:wne=\"http://schemas.microsoft.com/office/word/2006/wordml\"> \r\n");
		tempDocWriter.write(" <w:body> \r\n");
		tempDocWriter.flush();
	}
	
	protected void writeDocFooter(int pageWidth, int pageHeight) throws IOException
	{
		tempDocWriter.write("  <w:sectPr> \r\n");
		tempDocWriter.write("   <w:pgSz w:w=\"" + Utility.translatePointsToTwips(pageWidth) + "\" w:h=\"" + Utility.translatePointsToTwips(pageHeight) + "\" /> \r\n");
		tempDocWriter.write("   <w:pgMar w:top=\"0\" w:right=\"0\" w:bottom=\"0\" w:left=\"0\" w:header=\"0\" w:footer=\"0\" w:gutter=\"0\" /> \r\n");
//		tempDocWriter.write("   <w:cols w:space=\"720\" /> \r\n");
		tempDocWriter.write("   <w:docGrid w:linePitch=\"360\" /> \r\n");
		tempDocWriter.write("  </w:sectPr> \r\n");
		tempDocWriter.write(" </w:body> \r\n");
		tempDocWriter.write("</w:document> \r\n");
		tempDocWriter.flush();
	}
	
	protected abstract ExporterNature getExporterNature(ExporterFilter filter);

}


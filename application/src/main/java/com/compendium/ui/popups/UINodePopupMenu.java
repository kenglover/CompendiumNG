/********************************************************************************
 *                                                                              *
 *  (c) Copyright 2010 Verizon Communications USA and The Open University UK    *
 *                                                                              *
 *  This software is freely distributed in accordance with                      *
 *  the GNU Lesser General Public (LGPL) license, version 3 or later            *
 *  as published by the Free Software Foundation.                               *
 *  For details see LGPL: http://www.fsf.org/licensing/licenses/lgpl.html       *
 *               and GPL: http://www.fsf.org/licensing/licenses/gpl-3.0.html    *
 *                                                                              *
 *  This software is provided by the copyright holders and contributors "as is" *
 *  and any express or implied warranties, including, but not limited to, the   *
 *  implied warranties of merchantability and fitness for a particular purpose  *
 *  are disclaimed. In no event shall the copyright owner or contributors be    *
 *  liable for any direct, indirect, incidental, special, exemplary, or         *
 *  consequential damages (including, but not limited to, procurement of        *
 *  substitute goods or services; loss of use, data, or profits; or business    *
 *  interruption) however caused and on any theory of liability, whether in     *
 *  contract, strict liability, or tort (including negligence or otherwise)     *
 *  arising in any way out of the use of this software, even if advised of the  *
 *  possibility of such damage.                                                 *
 *                                                                              *
 ********************************************************************************/

package com.compendium.ui.popups;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.compendium.LanguageProperties;
import com.compendium.ProjectCompendium;
import com.compendium.core.CoreUtilities;
import com.compendium.core.ICoreConstants;
import com.compendium.core.datamodel.IModel;
import com.compendium.core.datamodel.ModelSessionException;
import com.compendium.core.datamodel.NodePosition;
import com.compendium.core.datamodel.NodeSummary;
import com.compendium.core.datamodel.PCSession;
import com.compendium.core.datamodel.ShortCutNodeSummary;
import com.compendium.core.datamodel.View;
import com.compendium.ui.ExecuteControl;
import com.compendium.ui.FormatProperties;
import com.compendium.ui.UIMapViewFrame;
import com.compendium.ui.UINode;
import com.compendium.ui.UIUtilities;
import com.compendium.ui.UIViewFrame;
import com.compendium.ui.UIViewPane;
import com.compendium.ui.dialogs.UIReadersDialog;
import com.compendium.ui.dialogs.UISendMailDialog;
import com.compendium.ui.dialogs.UITrashViewDialog;
import com.compendium.ui.plaf.NodeUI;
import com.compendium.ui.plaf.ViewPaneUI;

/**
 * This class draws and handles events for the right-click menu for nodes in a map
 *
 * @author	Mohammed Sajid Ali / Michelle Bachler / Lakshmi Prabhakaran
 */
public class UINodePopupMenu extends UIBaseMapPopupMenu implements ActionListener {
	/**
	 * class's own logger
	 */
	final Logger log = LoggerFactory.getLogger(getClass());
	/** The generated serial version id  */
	protected static final long serialVersionUID 		= 4134311162610834619L;

	/** The JMenuItem to create a new map and transclude the node associated with this popup into it.*/
	protected JMenuItem		miNewMap				= null;
	
	/** The menu item to format all transclusion of this node to this node's formatting.*/
	protected JMenuItem		miFormatTransclusions 	= null;

	/** The menu item to format all children and submaps of this node to this node's formatting.*/
	protected JMenuItem		miFormatAll 			= null;
	
	/** The JMenuItem which communicates with the meeting replay Jabber account.*/
	protected JMenuItem		miMeetingReplay			= null;

	/** The JJMenuItem which assigns the media index of the focused node to all other selected nodes.*/
	protected JMenuItem		miAssignMediaIndex		= null;
	
	/** The JMenuItem to open this node's contents dialog on the times tab.*/
	private JMenuItem		miMenuItemTimes			= null;

	/** A separator that can be turned off if required by simple menu.*/
	private JSeparator		separator				= null;

	/** The NodeUI object associated with this popup menu.*/
	protected NodeUI		oNode					= null;
	
	/** Holds the check data when looping to update formats.*/
	private	Hashtable  htCheckFormatNodes 			= new Hashtable();


	/**
	 * Constructor. Create the menus and items and draws the popup menu.
	 * @param title, the title for this popup menu.
	 * @param nodeui com.compendium.ui.plaf.NodeUI, the associated node for this popup menu.
	 */
	public UINodePopupMenu(String title, NodeUI nodeui) {
		super(title);
		setNode(nodeui);
		UIViewPane oViewPane = nodeui.getUINode().getViewPane();
		setViewPane(oViewPane);
		setViewPaneUI(oViewPane.getUI());
		init();
	}
	
	protected void init() {
		boolean bSimple = FormatProperties.simpleInterface;
		
		int nType = oNode.getUINode().getNode().getType();


		if (oViewPane.getView().getType() == ICoreConstants.MOVIEMAPVIEW) {
			miMenuItemTimes = new JMenuItem(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.times")); //$NON-NLS-1$
			miMenuItemTimes.addActionListener(this);
			add(miMenuItemTimes);
		}

		addContents();
		
		createNodeTypeChangeMenu();

		addSeparator();

		miNewMap = new JMenuItem(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.transcludeToNewMap")); //$NON-NLS-1$
		miNewMap.addActionListener(this);
		add(miNewMap);

		String sSource = oNode.getUINode().getNode().getSource();		
		if (!sSource.startsWith(ICoreConstants.sINTERNAL_REFERENCE)) {
			addInternalReference();				
			addSendToInbox();
		}
		
		miFormatTransclusions = new JMenuItem(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.applyFormatTransclusions")); //$NON-NLS-1$
		miFormatTransclusions.setToolTipText(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.applyFormatTranslucsionsTip")); //$NON-NLS-1$
		miFormatTransclusions.addActionListener(this);
		add(miFormatTransclusions);

		if (View.isViewType(nType)) {	
			miFormatAll = new JMenuItem(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.applyFormatDepth")); //$NON-NLS-1$
			miFormatAll.setToolTipText(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.applyFormatAll")); //$NON-NLS-1$
			miFormatAll.addActionListener(this);
			add(miFormatAll);
		}

		addSeparator();

		if (View.isViewType(nType) || View.isShortcutViewType(nType)) {
			View view = null;
			if (View.isShortcutViewType(nType)) {
				UINode uinode = oNode.getUINode();
				ShortCutNodeSummary nodesm = (ShortCutNodeSummary)uinode.getNode();
				view = (View)nodesm.getReferredNode();
			}
			else {
				view = (View)oNode.getUINode().getNode();
			} 

			try {view.initializeMembers();}
			catch(Exception ex) {log.error("Exception...", ex);}

			addReferences(view.getReferenceNodes());
		}

		addCopy(shortcutKey);
		addCut(shortcutKey);
		addDelete(shortcutKey);

		addSeparator();

		addImportMenu();
		addImportImage();
		addExportMenu();
		addSaveAsJPEG();
		
		addSeparator();
		
		addInternetSearch();
		
		addShortcut();
		addClone();
		addSeparator();
		
		addDelink();
	
		addBookmark();

		addSeparator();

		addClaiMakerMenu();

		addMoveLabelDetails();

		addSeparator();
		
		addReaders();
		
		addSeenUnseen();
		//Lakshmi (4/25/06) - if node is in read state enable mark unseen
		// and disable mark seen and vice versa
		int state = oNode.getUINode().getNode().getState();
		if(state == ICoreConstants.READSTATE){				
			showMarkUnseen = true;
		} else if(state == ICoreConstants.UNREADSTATE) {
			showMarkSeen = true;
		} else {
			showMarkUnseen = true;
			showMarkSeen = true;
		}
				
		separator = new JPopupMenu.Separator();
		add(separator);
	
		addProperties();
		addViews();

		/**
		 * If on the Mac OS and the Menu bar is at the top of the OS screen, remove the menu shortcut Mnemonics.
		 */
		if (ProjectCompendium.isMac && (FormatProperties.macMenuBar || (!FormatProperties.macMenuBar && !FormatProperties.macMenuUnderline)) )
			UIUtilities.removeMenuMnemonics(getSubElements());

		if (bSimple) {		
			addExtenderButton();
			setDisplay(bSimple);
		}

		pack();
		setSize(WIDTH,HEIGHT);
	}
	
	/**
	 * Hide/show items depending on whether the user wants the simple view or simple.
	 * @param bSimple
	 */
	protected void setDisplay(boolean bSimple) {
		if (bSimple) {
			miNewMap.setVisible(false);
			miFormatTransclusions.setVisible(false);
			if (miFormatAll != null)
				miFormatAll.setVisible(false);
			miImportXMLFlashmeeting.setVisible(false);
			miFileImport.setVisible(false);
			miImportCurrentView.setVisible(false);
			miImportMultipleViews.setVisible(false);
			miFavorites.setVisible(false);
			miMenuItemReaders.setVisible(false);
			miMenuItemMarkSeen.setVisible(false);
			miMenuItemMarkUnseen.setVisible(false);	
			separator.setVisible(false);	
			miMenuItemProperties.setVisible(false);				
		} else {
			miNewMap.setVisible(true);
			miFormatTransclusions.setVisible(true);
			if (miFormatAll != null)
				miFormatAll.setVisible(true);
			miImportXMLFlashmeeting.setVisible(true);
			miFileImport.setVisible(true);
			miImportCurrentView.setVisible(true);
			miImportMultipleViews.setVisible(true);
			miFavorites.setVisible(true);
			miMenuItemReaders.setVisible(true);
			if (showMarkSeen) {
				miMenuItemMarkSeen.setVisible(true);
			}
			if (showMarkUnseen) {
				miMenuItemMarkUnseen.setVisible(true);	
			}
			separator.setVisible(true);	
			miMenuItemProperties.setVisible(true);				
		}
		
		setControlItemStatus(bSimple);
		
		if (isVisible()) {
			setVisible(false);
			setVisible(true);
			requestFocus();
		}
	}

	/**
	 * Set the node associated with this popup menu.
	 * @param node com.compendium.ui.plaf.NodeUI, the node associated with this popup menu.
	 */
	public void setNode(NodeUI node) {
		oNode = node;
	}

	/**
	 * Handles the event of an option being selected.
	 * @param evt, the event associated with the option being selected.
	 */
	public void actionPerformed(ActionEvent evt) {

		Object source = evt.getSource();

		ProjectCompendium.APP.setWaitCursor();

		if (source.equals(miFormatTransclusions)) {
			formatTransclusions();
		} else if (source.equals(miFormatAll)) {			
			formatDecendants();
		} else if (source.equals(miNewMap)) {
			onNewMap();
		} else if(source.equals(miMenuItemTimes)) {
			oNode.getUINode().showTimeDialog();
		} else if (source.equals(miAssignMediaIndex)) {
			onAssignMediaIndex();
		} else if (source.equals(miMeetingReplay)) {
			oNode.getUINode().requestFocus();
		} else {
			super.actionPerformed(evt);
		}

		ProjectCompendium.APP.setDefaultCursor();
	}
	
	/**
	 * Assign the media index of the focused node to other selected nodes.
	 */
	private void onAssignMediaIndex() {

		UINode uiNode = oNode.getUINode();
		NodePosition oNodePos = uiNode.getNodePosition();
		IModel model = ProjectCompendium.APP.getModel(); 
		oNodePos.initialize(model.getSession(), model);		
		String id = uiNode.getNode().getId();

	}
	
	/**
	 * Search Google using this node's label.
	 */
	protected void searchGoogle() {

		if (!ProjectCompendium.InternetSearchAllowed) {
			return;
		} else {

			String sLabel = oNode.getUINode().getText();
			try {
				sLabel = CoreUtilities.cleanURLText(sLabel);
			} catch (Exception e) {
				log.error("Exception...", e);
			}

			ExecuteControl.launch(ProjectCompendium.InternetSearchProviderUrl + sLabel); //$NON-NLS-1$
			oNode.getUINode().requestFocus();
		}
	}	
	
	/**
	 * Search ClaiMaker concepts using this node's label.
	 */
	protected void searchClaiMakerConcepts() {
		String sLabel = oNode.getUINode().getText();
		try {
			sLabel = CoreUtilities.cleanURLText(sLabel);
		} catch (Exception e) {}
		ExecuteControl.launch( claiMakerServer+"search-concept.php?op=search&inputWord="+sLabel ); //$NON-NLS-1$
		oNode.getUINode().requestFocus();
	}

	/**
	 * Search ClaiMaker neighbourhood using this node's label.
	 */
	protected void searchClaiMakerNeighbourhood() {
		String sLabel = oNode.getUINode().getText();
		try {
			sLabel = CoreUtilities.cleanURLText(sLabel);
		} catch (Exception e) {}
		ExecuteControl.launch( claiMakerServer+"discover/neighborhood.php?op=search&concept="+sLabel ); //$NON-NLS-1$
		oNode.getUINode().requestFocus();
	}

	/**
	 * Search ClaiMaker documents using this node's label.
	 */
	protected void searchClaiMakerDocuments() {
		String sLabel = oNode.getUINode().getText();
		try {
			sLabel = CoreUtilities.cleanURLText(sLabel);
		} catch (Exception e) {}
		ExecuteControl.launch( claiMakerServer+"search-document.php?op=search&Title="+sLabel ); //$NON-NLS-1$
		oNode.getUINode().requestFocus();
	}
	
	/**
	 * Display a list of all users who have read this node.
	 */
	protected void displayReaders() {
		//Lakshmi (4/19/06) - code added to display Readers list Dialog
		String nodeId = oNode.getUINode().getNode().getId();
		UIReadersDialog readers = new UIReadersDialog(ProjectCompendium.APP, nodeId);
		UIUtilities.centerComponent(readers, ProjectCompendium.APP);
		readers.setVisible(true);		
	}
	
	/**
	 * Open the contents dialog for the given context.
	 */
	protected void openContents() {
		// open the node
		if(oNode.getUINode().getNode().getType() == ICoreConstants.TRASHBIN) {
			UITrashViewDialog dlgTrash = new UITrashViewDialog(ProjectCompendium.APP, oNode);
			UIUtilities.centerComponent(dlgTrash, ProjectCompendium.APP);
			dlgTrash .setVisible(true);
		}
		else {
			oNode.openEditDialog(false);
		}
		oNode.getUINode().requestFocus();
	}

	/**
	 * Open the contents dialog for the given context on the properties tab.
	 * Subclasses must implement this method.
	 */
	protected void openProperties() {
		(oNode.getUINode()).showPropertiesDialog();
	}

	/**
	 * Open the contents dialog for the given context on the views tab.
	 * Subclasses must implement this method.
	 */
	protected void openViews() {
		oViewPane.getViewFrame().showViewsDialog();
	}
	
	/**
	 * Aplly this nodes formatting to any transclusions of this node.
	 */
	protected void formatTransclusions() {
   		int answer = JOptionPane.showConfirmDialog(this, 
   				LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warningMessage1a")+"\n\n"+LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warningMessage1b")+"\n", 
   				LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warning"), //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

		if (answer == JOptionPane.YES_OPTION) {							
			NodePosition pos = oNode.getUINode().getNodePosition();
			IModel oModel = ProjectCompendium.APP.getModel();
			PCSession oSession = oModel.getSession();
			try {
				oModel.getViewService().updateTransclusionFormatting(oSession, pos.getNode().getId(),
					new Date(), pos.isShowTags(), pos.ishowText(), pos.isShowTransclusions(), 
					pos.isShowWeight(), pos.isShowSmallIcon(), pos.isHideIcon(),
					pos.getLabelWrapWidth(), pos.getFontSize(), pos.getFontFace(),
					pos.getFontStyle(), pos.getForeground(), pos.getBackground());
			} catch (SQLException e) {
				ProjectCompendium.APP.displayError(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warningMessage2")+"\n\n"+e.getMessage()); //$NON-NLS-1$
			}
			
			try {
				NodeSummary node = oNode.getUINode().getNode();
				JInternalFrame[] frames = ProjectCompendium.APP.getDesktop().getAllFrames();
				for(int i=0; i<frames.length; i++) {
					UIViewFrame viewFrame = (UIViewFrame)frames[i];
					if (viewFrame instanceof UIMapViewFrame) {
						UIViewPane pane = ((UIMapViewFrame)viewFrame).getViewPane();
						UINode uinode = (UINode)pane.get(node.getId());
						if (uinode != null) {
							NodePosition npos = uinode.getNodePosition();
							npos.setShowTags(pos.isShowTags());
							npos.setShowText(pos.ishowText());
							npos.setShowTrans(pos.isShowTransclusions());
							npos.setShowWeight(pos.isShowWeight());
							npos.setShowSmallIcon(pos.isShowSmallIcon());
							npos.setHideIcon(pos.isHideIcon());
							npos.setLabelWrapWidth(pos.getLabelWrapWidth());
							npos.setFontSize(pos.getFontSize());
							npos.setFontStyle(pos.getFontStyle());
							npos.setFontFace(pos.getFontFace());
							npos.setForeground(pos.getForeground());
							npos.setBackground(pos.getBackground());							
						}
					}
				}
			} catch(Exception ex) {
				log.error("Exception...", ex);					
			}
		}		
	}

	/**
	 * Apply this nodes formatting to all its child nodes recursively 
	 * (only available on view nodes) 
	 */
	protected void formatDecendants() {
   		int answer = JOptionPane.showConfirmDialog(this, 
   				LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warningMessage3a") +"\n\n"+  //$NON-NLS-1$ //$NON-NLS-2$
   				LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warningMessage3b") +"\n"+ //$NON-NLS-1$ //$NON-NLS-2$
   				LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warningMessage3c") +"\n",  //$NON-NLS-1$ //$NON-NLS-2$
   				LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.warning"), //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

		if (answer == JOptionPane.YES_OPTION) {							
			View view = (View)oNode.getUINode().getNode();
			NodePosition mainpos = oNode.getUINode().getNodePosition();		
			
			String sFontFace = mainpos.getFontFace();
			int nFontSize = mainpos.getFontSize();
			int nFontStyle = mainpos.getFontStyle();
			int nBackground = mainpos.getBackground();
			int nForeground = mainpos.getForeground();
			int nWrapWidth = mainpos.getLabelWrapWidth();
			boolean bShowTags = mainpos.isShowTags();
			boolean bShowText = mainpos.ishowText();
			boolean bShowTrans = mainpos.isShowTransclusions();
			boolean bShowWeight = mainpos.isShowWeight();
			boolean bSmallIcon = mainpos.isShowSmallIcon();
			boolean bHideIcon = mainpos.isHideIcon();
			
			IModel model = ProjectCompendium.APP.getModel();
			PCSession session = model.getSession();

			String sViewID = view.getId();
			
			htCheckFormatNodes.clear();
			htCheckFormatNodes.put(sViewID, sViewID);
			
			try {
				Vector vtNodes = new Vector();
				Enumeration e = view.getPositions();		
				int count = vtNodes.size();
				
				NodePosition pos = null;
				String sNextID = ""; 		 //$NON-NLS-1$
				for (Enumeration nodes = e; nodes.hasMoreElements();) {
					pos = (NodePosition) nodes.nextElement();
					sNextID = pos.getNode().getId();
				
					model.getViewService().updateFormatting(session, sViewID, sNextID,
								new Date(), bShowTags, bShowText, bShowTrans, bShowWeight, 
									bSmallIcon, bHideIcon, nWrapWidth, nFontSize, sFontFace,
										nFontStyle, nForeground, nBackground);
									
					pos.setBackground(nBackground);
					pos.setFontFace(sFontFace);
					pos.setFontStyle(nFontStyle);
					pos.setFontSize(nFontSize);
					pos.setForeground(nForeground);
					pos.setHideIcon(bHideIcon);
					pos.setLabelWrapWidth(nWrapWidth);
					pos.setShowSmallIcon(bSmallIcon);
					pos.setShowTags(bShowTags);
					pos.setShowText(bShowText);
					pos.setShowTrans(bShowTrans);
					pos.setShowWeight(bShowWeight);
					
					if (pos.getNode() instanceof View && !htCheckFormatNodes.containsKey(sNextID)) {
						this.setFormattting((View)pos.getNode(), bShowTags, bShowText, bShowTrans, bShowWeight, 
								bSmallIcon, bHideIcon, nWrapWidth, nFontSize, sFontFace,
								nFontStyle, nForeground, nBackground);
					}
				}
				
			} catch (SQLException e) {
				ProjectCompendium.APP.displayError(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.errorFormatUpdate")+"\n\n"+e.getMessage()); //$NON-NLS-1$
			} catch (ModelSessionException ex) {
				ProjectCompendium.APP.displayError(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UINodePopupMenu.errorFormatUpdate")+"\n\n"+ex.getMessage());					 //$NON-NLS-1$
			}
		}		
	}
	
	/**
	 * Set the formatting of the past view and its child nodes and views to full depth to the pass paramters.
	 * @param view
	 * @param bShowTags
	 * @param bShowText
	 * @param bShowTrans
	 * @param bShowWeight
	 * @param bSmallIcon
	 * @param bHideIcon
	 * @param nWrapWidth
	 * @param nFontSize
	 * @param sFontFace
	 * @param nFontStyle
	 * @param nForeground
	 * @param nBackground
	 */
	private void setFormattting(View view, boolean bShowTags,
			boolean bShowText, boolean bShowTrans, boolean bShowWeight, boolean bSmallIcon,
			boolean bHideIcon, int nWrapWidth, int nFontSize, String sFontFace,
			int nFontStyle, int nForeground, int nBackground) throws ModelSessionException, SQLException {
		
		IModel model = ProjectCompendium.APP.getModel();
		PCSession session = model.getSession();
		
		if (!view.isMembersInitialized()) {
			view.initialize(model.getSession(), model);
			view.initializeMembers();				
		}
		
		String sViewID = view.getId();
		htCheckFormatNodes.put(sViewID, sViewID);
		
		Enumeration e = view.getPositions();					
		NodePosition pos = null;
		String sNextID = ""; 		 //$NON-NLS-1$
		for (Enumeration nodes = e; nodes.hasMoreElements();) {
			pos = (NodePosition) nodes.nextElement();
			sNextID = pos.getNode().getId();
			
			model.getViewService().updateFormatting(session, sViewID, sNextID,
			new Date(), bShowTags, bShowText, bShowTrans, bShowWeight, 
			bSmallIcon, bHideIcon, nWrapWidth, nFontSize, sFontFace,
			nFontStyle, nForeground, nBackground);
							
			pos.setBackground(nBackground);
			pos.setFontFace(sFontFace);
			pos.setFontStyle(nFontStyle);
			pos.setFontSize(nFontSize);
			pos.setForeground(nForeground);
			pos.setHideIcon(bHideIcon);
			pos.setLabelWrapWidth(nWrapWidth);
			pos.setShowSmallIcon(bSmallIcon);
			pos.setShowTags(bShowTags);
			pos.setShowText(bShowText);
			pos.setShowTrans(bShowTrans);
			pos.setShowWeight(bShowWeight);
			
			if (pos.getNode() instanceof View && !htCheckFormatNodes.containsKey(sNextID)) {
				this.setFormattting((View)pos.getNode(), bShowTags, bShowText, bShowTrans, bShowWeight, 
						bSmallIcon, bHideIcon, nWrapWidth, nFontSize, sFontFace,
						nFontStyle, nForeground, nBackground);
			}
		}
	}
	
	/**
	 * Create a recipient dialog box so the user can choose multiple people to send the INR to
	*/
	protected void sendToInbox() {
			
		View oHomeView = ProjectCompendium.APP.getHomeView();
		if (oHomeView.getId().equals(oViewPane.getView().getId())) {
			JOptionPane.showMessageDialog(this, 
					LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UIBasePopupMenu.inBoxError"), //$NON-NLS-1$
					LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UIBasePopupMenu.inBoxErrorTitle"), //$NON-NLS-1$
					JOptionPane.INFORMATION_MESSAGE);
			return;
		} 
		
		UISendMailDialog dlg = new UISendMailDialog(ProjectCompendium.APP, 
													oNode.getUINode().getViewPane().getView(),
													oNode.getUINode().getNode());
		UIUtilities.centerComponent(dlg, ProjectCompendium.APP);
		dlg.setVisible(true);

	}
	
	/**
	 * Create a Reference node with internal link to this node.
	 */
	protected void createInternalLink() {

		UINode uinode = oNode.getUINode();		
		double scale = uinode.getScale();

		UINode newNode = null;

		UIViewPane oViewPane = uinode.getViewPane();
		View view = oViewPane.getView();
		
		String sRef = ICoreConstants.sINTERNAL_REFERENCE+view.getId()+"/"+uinode.getNode().getId(); //$NON-NLS-1$

		// Do all calculations at 100% scale and then scale back down if required.
		if (oViewPane != null) {
			try {
				if (scale != 1.0) {
					oViewPane.scaleNode(uinode, 1.0);
				}
				
				ViewPaneUI oViewPaneUI = oViewPane.getUI();
				if (oViewPaneUI != null) {
	
					int parentHeight = uinode.getHeight();
					int parentWidth = uinode.getWidth();
	
					Point loc = uinode.getNodePosition().getPos();
					loc.x += parentWidth;
					loc.x += 100;
	
					// CREATE NEW NODE RIGHT OF THE GIVEN NODE WITH THE GIVEN LABEL
					newNode = oViewPaneUI.createNode(ICoreConstants.REFERENCE,
									 "", //$NON-NLS-1$
									 ProjectCompendium.APP.getModel().getUserProfile().getUserName(),
									 LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UIBasePopupMenu.goto")+": "+uinode.getText(), //$NON-NLS-1$
									 LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UIBasePopupMenu.inview")+": "+view.getLabel(), //$NON-NLS-1$ //$NON-NLS-2$
									 loc.x,
									 loc.y,
									 sRef
									 );
					
					if (scale != 1.0) {
						oViewPane.scaleNode(newNode, 1.0);
					}
	
					//Adjust y location for height variation so new node centered.
					int childHeight = newNode.getHeight();
	
					int locy = 0;
					if (parentHeight > childHeight) {
						locy = loc.y + ((parentHeight-childHeight)/2);
					}
					else if (childHeight > parentHeight) {
						locy = loc.y - ((childHeight-parentHeight)/2);
					}
	
					if (locy > 0 && locy != loc.y) {
						loc.y = locy;
						(newNode.getNodePosition()).setPos(loc);
						try {
							oViewPane.getView().setNodePosition(newNode.getNode().getId(), loc);
						}
						catch(Exception ex) {
							log.info(ex.getMessage());
						}
					}
					if (scale != 1.0) {
						oViewPane.scaleNode(newNode, scale);
					}
				}
				
				if (scale != 1.0) {
					oViewPane.scaleNode(uinode, scale);
				}			
			} catch (Exception e) {
				ProjectCompendium.APP.displayError(LanguageProperties.getString(LanguageProperties.POPUPS_BUNDLE, "UIBasePopupMenu.errorMessageIternalLink")+"\n\n"+e.getLocalizedMessage());							 //$NON-NLS-1$
			}
		}
	}		
	
	/**
	 * Create a Reference node with internal link to this node.
	 */
	protected void createBookmark() {
		UINode uinode = oNode.getUINode();	
		NodeSummary node = uinode.getNode();
		UIViewPane oViewPane = uinode.getViewPane();
		View view = oViewPane.getView();		
		ProjectCompendium.APP.createFavorite(node.getId(), view.getId(), view.getLabel()+"&&&"+node.getLabel(), node.getType()); //$NON-NLS-1$
		oNode.getUINode().requestFocus();		
	}
	
	/**
	 * Transclude selected nodes into new maps and link maps to their respective selected nodes.
	 */
	protected void onNewMap() {
		int count = 0;
		for(Enumeration e = oViewPane.getSelectedNodes();e.hasMoreElements();) {
			count++;
			UINode uinode = (UINode)e.nextElement();

			UINode newMap = UIUtilities.createNodeAndLink(UIUtilities.DIRECTION_RIGHT, uinode, ICoreConstants.MAPVIEW, 100, uinode.getText(), ProjectCompendium.APP.getModel().getUserProfile().getUserName(), ICoreConstants.RESPONDS_TO_LINK);

			if (newMap != null) {
				int x = 10;
				int y = 250;
				try {							
					((View)newMap.getNode()).addNodeToView(uinode.getNode(), x, y);
				}
				catch(Exception ex) {
					log.error("Error: (UINodePopupMenu.onNewMap)\n\n", ex);
				}
			}
		}
	}
}

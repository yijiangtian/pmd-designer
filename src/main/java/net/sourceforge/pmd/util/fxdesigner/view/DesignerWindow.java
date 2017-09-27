/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.view;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import org.fxmisc.richtext.LineNumberFactory;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.DesignerWindowPresenter;
import net.sourceforge.pmd.util.fxdesigner.model.MetricResult;
import net.sourceforge.pmd.util.fxdesigner.util.LogEntry;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.CustomCodeArea;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

/**
 * View class for the designer window.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class DesignerWindow implements Initializable {

    @FXML
    private Label metricsTitleLabel;
    @FXML
    private ToggleButton bottomTabsToggle;
    @FXML
    private TabPane bottomTabPane;

    @FXML
    private Tab eventLogTab;
    @FXML
    private TableView<LogEntry> eventLogTableView;
    @FXML
    private TableColumn<LogEntry, Date> logDateColumn;
    @FXML
    private TableColumn<LogEntry, Date> logMessageColumn;
    @FXML
    private TableColumn<LogEntry, Date> logCategoryColumn;
    @FXML
    private TextArea logDetailsTextArea;

    @FXML
    private ChoiceBox<String> xpathVersionChoiceBox;
    @FXML
    private Tab metricResultsTitledPane;
    @FXML
    private MenuItem openFileMenuItem;
    @FXML
    private MenuItem licenseMenuItem;
    @FXML
    private TreeView<Object> scopeHierarchyTreeView;
    @FXML
    private Menu openRecentMenu;
    @FXML
    private Menu exportMenu;
    @FXML
    private MenuItem exportToTestCodeMenuItem;
    @FXML
    private MenuItem exportXPathMenuItem;
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem toggleSyntaxHighlighting;
    @FXML
    private CustomCodeArea codeEditorArea;
    @FXML
    private ChoiceBox<LanguageVersion> languageChoiceBox;
    @FXML
    private Button refreshASTButton;
    @FXML
    private CustomCodeArea xpathExpressionArea;
    @FXML
    private ListView<Node> xpathResultListView;
    @FXML
    private ListView<String> xpathAttributesListView;
    @FXML
    private TitledPane violationsTitledPane;
    @FXML
    private TreeView<Node> astTreeView;
    @FXML
    private Tab xpathEditorTab;
    @FXML
    private SplitPane editorPanelHorizontalSplitPane;
    @FXML
    private Tab xpathAttributesTitledPane;
    @FXML
    private Accordion nodeInfoAccordion;
    @FXML
    private BorderPane editorAndASTBorderPane;
    @FXML
    private TitledPane astTitledPane;
    @FXML
    private SplitPane mainVerticalSplitPane;
    @FXML
    private ListView<MetricResult> metricResultsListView;
    /* */
    private StringProperty sourceCodeProperty;
    private BooleanProperty isSyntaxHighlightingEnabled = new SimpleBooleanProperty(false);
    private BooleanProperty isBottomPaneExpandedProperty = new SimpleBooleanProperty(true);


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeView();
        new DesignerWindowPresenter(this).initialize(); // instantiate presenter
    }


    private void initializeView() {

        sourceCodeProperty = new SimpleStringProperty();
        sourceCodeProperty.bind(codeEditorArea.textProperty());

        Binding<Boolean> bottomPaneBinding
            = Bindings.createBooleanBinding(() -> bottomTabsToggle.isSelected(),
                                            bottomTabsToggle.selectedProperty());

        isBottomPaneExpandedProperty.bind(bottomPaneBinding);

        codeEditorArea.setParagraphGraphicFactory(LineNumberFactory.get(codeEditorArea));


        final double defaultMainHorizontalSplitPaneDividerPosition
            = editorPanelHorizontalSplitPane.getDividerPositions()[0];


        // show/ hide bottom pane
        isBottomPaneExpandedProperty.addListener((observable, wasExpanded, isNowExpanded) -> {
            KeyValue keyValue = null;
            DoubleProperty divPosition = editorPanelHorizontalSplitPane.getDividers().get(0).positionProperty();
            if (wasExpanded && !isNowExpanded) {
                keyValue = new KeyValue(divPosition, 1);
            } else if (!wasExpanded && isNowExpanded) {
                keyValue = new KeyValue(divPosition, defaultMainHorizontalSplitPaneDividerPosition);
            }

            if (keyValue != null) {
                Timeline timeline = new Timeline(new KeyFrame(Duration.millis(200), keyValue));
                timeline.play();
            }
        });
    }


    public void notifyMetricsAvailable(long numMetrics) {
        metricResultsTitledPane.setText("Metrics\t(" + (numMetrics == 0 ? "none" : numMetrics) + ")");
        metricsTitleLabel.setText("Metrics\t(" + (numMetrics == 0 ? "none" : numMetrics) + " available)");
        metricResultsTitledPane.setDisable(numMetrics == 0);
    }


    public void notifyOutdatedAST() {
        astTitledPane.setText("Abstract syntax tree (outdated)");
    }


    public void acknowledgeUpdatedAST() {
        astTitledPane.setText("Abstract syntax tree");
    }


    public void displayXPathResultsSize(int size) {
        violationsTitledPane.setText("Matched nodes\t(" + size + ")");
    }


    public void displayXPathError(Throwable t) {
        // Currently dismisses the exception
        violationsTitledPane.setText("Matched nodes\t(error)");
    }


    public TreeView<Object> getScopeHierarchyTreeView() {
        return scopeHierarchyTreeView;
    }


    public CustomCodeArea getCodeEditorArea() {
        return codeEditorArea;
    }


    public StringProperty sourceCodeProperty() {
        return sourceCodeProperty;
    }


    public ChoiceBox<LanguageVersion> getLanguageChoiceBox() {
        return languageChoiceBox;
    }


    public Button getRefreshASTButton() {
        return refreshASTButton;
    }


    public CustomCodeArea getXpathExpressionArea() {
        return xpathExpressionArea;
    }


    public ListView<Node> getXpathResultListView() {
        return xpathResultListView;
    }


    public ListView<String> getXpathAttributesListView() {
        return xpathAttributesListView;
    }


    public TitledPane getViolationsTitledPane() {
        return violationsTitledPane;
    }


    public TreeView<Node> getAstTreeView() {
        return astTreeView;
    }


    public BorderPane getMainEditorBorderPane() {
        return editorAndASTBorderPane;
    }


    public Tab getXpathEditorTab() {
        return xpathEditorTab;
    }


    public SplitPane getMainVerticalSplitPane() {
        return mainVerticalSplitPane;
    }


    public ListView<MetricResult> getMetricResultsListView() {
        return metricResultsListView;
    }


    public MenuItem getLoadSourceFromFileMenuItem() {
        return openFileMenuItem;
    }


    public MenuItem getLicenseMenuItem() {
        return licenseMenuItem;
    }


    public Menu getFileMenu() {
        return fileMenu;
    }


    public MenuItem getExportToTestCodeMenuItem() {
        return exportToTestCodeMenuItem;
    }


    public MenuItem getExportXPathMenuItem() {
        return exportXPathMenuItem;
    }


    public Menu getOpenRecentMenu() {
        return openRecentMenu;
    }


    @FXML
    public void onToggleSyntaxHighlightingClicked(Event event) {
        isSyntaxHighlightingEnabled.set(!isSyntaxHighlightingEnabled.get());
        toggleSyntaxHighlighting.setText((isSyntaxHighlightingEnabled.get() ? "Disable" : "Enable")
                                             + " syntax highlighting");
    }


    public BooleanProperty isSyntaxHighlightingEnabledProperty() {
        return isSyntaxHighlightingEnabled;
    }


    public ChoiceBox<String> getXpathVersionChoiceBox() {
        return xpathVersionChoiceBox;
    }


    public Tab getEventLogTab() {
        return eventLogTab;
    }


    public TableView<LogEntry> getEventLogTableView() {
        return eventLogTableView;
    }


    public TableColumn<LogEntry, Date> getLogDateColumn() {
        return logDateColumn;
    }


    public TableColumn<LogEntry, Date> getLogMessageColumn() {
        return logMessageColumn;
    }


    public TableColumn<LogEntry, Date> getLogCategoryColumn() {
        return logCategoryColumn;
    }


    public TextArea getLogDetailsTextArea() {
        return logDetailsTextArea;
    }


    public TabPane getBottomTabPane() {
        return bottomTabPane;
    }


    public boolean isBottomPaneExpanded() {
        return isBottomPaneExpandedProperty.get();
    }


    public BooleanProperty isBottomPaneExpandedPropertyProperty() {
        return isBottomPaneExpandedProperty;
    }


    public ToggleButton getBottomTabsToggle() {
        return bottomTabsToggle;
    }


}

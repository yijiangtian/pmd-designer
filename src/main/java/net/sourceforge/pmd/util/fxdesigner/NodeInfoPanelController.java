/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.parentIterator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.SuspendableEventStream;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.lang.metrics.LanguageMetricsProvider;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.model.MetricResult;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ScopeHierarchyTreeCell;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ScopeHierarchyTreeItem;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


/**
 * Controller of the node info panel (left).
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class NodeInfoPanelController extends AbstractController implements NodeSelectionSource {



    @FXML
    private ToolbarTitledPane metricsTitledPane;
    @FXML
    private TabPane nodeInfoTabPane;
    @FXML
    private Tab metricResultsTab;
    @FXML
    private ListView<MetricResult> metricResultsListView;
    @FXML
    private TreeView<Object> scopeHierarchyTreeView;


    @FXML
    private NodeDetailPaneController nodeDetailsTabController;


    private Node selectedNode;

    private SuspendableEventStream<TreeItem<Object>> myScopeItemSelectionEvents;


    public NodeInfoPanelController(DesignerRoot designerRoot) {
        super(designerRoot);
    }



    @Override
    protected void beforeParentInit() {


        scopeHierarchyTreeView.setCellFactory(view -> new ScopeHierarchyTreeCell());


        // suppress as early as possible in the pipeline
        myScopeItemSelectionEvents = EventStreams.valuesOf(scopeHierarchyTreeView.getSelectionModel().selectedItemProperty()).suppressible();

        EventStream<NodeSelectionEvent> selectionEvents = myScopeItemSelectionEvents.filter(Objects::nonNull)
                                                                                    .map(TreeItem::getValue)
                                                                                    .filterMap(o -> o instanceof NameDeclaration, o -> (NameDeclaration) o)
                                                                                    .map(NameDeclaration::getNode)
                                                                                    .map(NodeSelectionEvent::of);

        // TODO split this into independent NodeSelectionSources
        initNodeSelectionHandling(getDesignerRoot(), selectionEvents, true);
    }



    /**
     * Displays info about a node. If null, the panels are reset.
     *
     * @param node Node to inspect
     * @param options
     */
    @Override
    public void setFocusNode(Node node, Set<SelectionOption> options) {
        if (node == null) {
            invalidateInfo();
            return;
        }

        if (node.equals(selectedNode)) {
            return;
        }
        selectedNode = node;

        displayAttributes(node);
        displayMetrics(node);
        displayScopes(node);
    }


    private void displayAttributes(Node node) {
    }


    private void displayMetrics(Node node) {
        ObservableList<MetricResult> metrics = evaluateAllMetrics(node);
        metricResultsListView.setItems(metrics);
        notifyMetricsAvailable(metrics.stream()
                                      .map(MetricResult::getValue)
                                      .filter(result -> !result.isNaN())
                                      .count());
    }


    private void displayScopes(Node node) {

        // current selection
        TreeItem<Object> previousSelection = scopeHierarchyTreeView.getSelectionModel().getSelectedItem();

        ScopeHierarchyTreeItem rootScope = ScopeHierarchyTreeItem.buildAscendantHierarchy(node);
        scopeHierarchyTreeView.setRoot(rootScope);

        if (previousSelection != null) {
            // Try to find the node that was previously selected and focus it in the new ascendant hierarchy.
            // Otherwise, when you select a node in the scope tree, since focus of the app is shifted to that
            // node, the scope hierarchy is reset and you lose the selection - even though obviously the node
            // you selected is in its own scope hierarchy so it looks buggy.
            int maxDepth = DesignerIteratorUtil.count(parentIterator(previousSelection, true));
            rootScope.tryFindNode(previousSelection.getValue(), maxDepth)
                     // suspend notifications while selecting
                     .ifPresent(item -> myScopeItemSelectionEvents.suspendWhile(() -> scopeHierarchyTreeView.getSelectionModel().select(item)));
        }
    }

    /**
     * Invalidates the info being displayed.
     */
    private void invalidateInfo() {
        metricResultsListView.setItems(FXCollections.emptyObservableList());
        scopeHierarchyTreeView.setRoot(null);
    }


    private void notifyMetricsAvailable(long numMetrics) {
        metricResultsTab.setText("Metrics\t(" + (numMetrics == 0 ? "none" : numMetrics) + ")");
        metricsTitledPane.setTitle("Metrics\t(" + (numMetrics == 0 ? "none" : numMetrics) + " available)");
        metricResultsTab.setDisable(numMetrics == 0);
    }


    private ObservableList<MetricResult> evaluateAllMetrics(Node n) {
        LanguageMetricsProvider<?, ?> provider = getGlobalLanguageVersion().getLanguageVersionHandler().getLanguageMetricsProvider();
        if (provider == null) {
            return FXCollections.emptyObservableList();
        }
        List<MetricResult> resultList =
            provider.computeAllMetricsFor(n)
                    .entrySet()
                    .stream()
                    .map(e -> new MetricResult(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        return FXCollections.observableArrayList(resultList);
    }


    @Override
    public String getDebugName() {
        return "info-panel";
    }
}

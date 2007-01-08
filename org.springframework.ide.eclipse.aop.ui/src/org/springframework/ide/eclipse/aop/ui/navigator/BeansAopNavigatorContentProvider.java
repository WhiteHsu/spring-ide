/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.ide.eclipse.aop.ui.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.document.ElementImpl;
import org.springframework.ide.eclipse.aop.core.model.IAopProject;
import org.springframework.ide.eclipse.aop.core.model.IAopReference;
import org.springframework.ide.eclipse.aop.core.model.IAspectDefinition;
import org.springframework.ide.eclipse.aop.ui.Activator;
import org.springframework.ide.eclipse.aop.ui.navigator.model.AdviceRootAopReferenceNode;
import org.springframework.ide.eclipse.aop.ui.navigator.model.AdvisedRootAopReferenceNode;
import org.springframework.ide.eclipse.aop.ui.navigator.model.BeanReferenceNode;
import org.springframework.ide.eclipse.aop.ui.navigator.model.IReferenceNode;
import org.springframework.ide.eclipse.aop.ui.navigator.model.JavaElementReferenceNode;
import org.springframework.ide.eclipse.beans.core.BeansCorePlugin;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansModelUtils;
import org.springframework.ide.eclipse.beans.core.model.IBean;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfig;
import org.springframework.ide.eclipse.beans.core.model.IBeansProject;
import org.springframework.ide.eclipse.core.io.ZipEntryStorage;
import org.springframework.ide.eclipse.core.model.IModelChangeListener;
import org.springframework.ide.eclipse.core.model.IModelElement;
import org.springframework.ide.eclipse.core.model.ModelChangeEvent;

/**
 */
@SuppressWarnings("restriction")
public class BeansAopNavigatorContentProvider implements
        ICommonContentProvider, IModelChangeListener {

    @SuppressWarnings("unused")
    private INavigatorContentExtension contentExtension;

    private StructuredViewer viewer;

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (viewer instanceof StructuredViewer) {
            this.viewer = (StructuredViewer) viewer;

            if (oldInput == null && newInput != null) {
                BeansCorePlugin.getModel().addChangeListener(this);
            }
            else if (oldInput != null && newInput == null) {
                BeansCorePlugin.getModel().removeChangeListener(this);
            }
        }
        else {
            this.viewer = null;
        }
    }

    public void dispose() {
        if (viewer != null && viewer.getInput() != null) {
            BeansCorePlugin.getModel().removeChangeListener(this);
        }
    }

    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof IType) {
            return new Object[] { new JavaElementReferenceNode(
                    (IType) inputElement, true) };
        }
        else if (inputElement instanceof SourceMethod) {
            return getChildren(inputElement);
        }
        else if (inputElement instanceof JavaElementReferenceNode) {
            return getChildren(((JavaElementReferenceNode) inputElement)
                    .getJavaElement());
        }
        else if (inputElement instanceof ElementImpl) {
            return getChildren(inputElement);
        }
        return IModelElement.NO_CHILDREN;
    }

    @SuppressWarnings("restriction")
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IReferenceNode) {
            return ((IReferenceNode) parentElement).getChildren();
        }
        else if (parentElement instanceof JavaElementReferenceNode
                && ((JavaElementReferenceNode) parentElement).isRoot()) {
            return getChildren(((JavaElementReferenceNode) parentElement)
                    .getJavaElement());
        }
        else if (parentElement instanceof IType) {
            IType type = (IType) parentElement;
            List<Object> me = new ArrayList<Object>();
            try {
                IMethod[] methods = type.getMethods();
                for (IMethod method : methods) {
                    if (Activator.getModel().isAdvice(method)
                            || Activator.getModel().isAdvised(method)) {
                        me.addAll(Arrays.asList(getChildren(method)));
                    }
                }
            }
            catch (JavaModelException e) {
            }

            List<IBean> beansFound = new ArrayList<IBean>();
            for (int i = 0; i < me.size(); i++) {
                if (me.get(i) instanceof AdvisedRootAopReferenceNode) {
                    beansFound.add(((AdvisedRootAopReferenceNode) me.get(i))
                            .getBean());
                }
            }

            // add normal beans
            IBeansProject beanProject = BeansCorePlugin.getModel().getProject(
                    type.getJavaProject().getProject());
            if (beanProject != null) {
                Set<IBean> beans = beanProject.getBeans(type
                        .getFullyQualifiedName());
                for (IBean bean : beans) {
                    if (!beansFound.contains(bean)) {
                        me.add(new BeanReferenceNode(bean, false));
                    }
                }
            }
            return me.toArray();
        }
        else if (parentElement instanceof IMethod
                && parentElement instanceof SourceMethod) {
            IMethod method = (IMethod) parentElement;
            IAopProject project = Activator.getModel().getProject(
                    method.getJavaProject().getProject());
            if (project != null && project.getAllReferences().size() > 0) {
                List<IAopReference> references = project.getAllReferences();
                Map<IAspectDefinition, List<IAopReference>> foundSourceReferences = new HashMap<IAspectDefinition, List<IAopReference>>();
                Map<IBean, List<IAopReference>> foundTargetReferences = new HashMap<IBean, List<IAopReference>>();
                for (IAopReference reference : references) {
                    if (reference.getTarget().equals(method)) {
                        if (foundTargetReferences.containsKey(reference
                                .getTargetBean())) {
                            foundTargetReferences
                                    .get(reference.getTargetBean()).add(
                                            reference);
                        }
                        else {
                            List<IAopReference> tmp = new ArrayList<IAopReference>();
                            tmp.add(reference);
                            foundTargetReferences.put(
                                    reference.getTargetBean(), tmp);
                        }
                    }
                    if (reference.getSource().equals(method)) {
                        if (foundSourceReferences.containsKey(reference
                                .getDefinition())) {
                            foundSourceReferences
                                    .get(reference.getDefinition()).add(
                                            reference);
                        }
                        else {
                            List<IAopReference> tmp = new ArrayList<IAopReference>();
                            tmp.add(reference);
                            foundSourceReferences.put(
                                    reference.getDefinition(), tmp);
                        }
                    }
                }
                List<IReferenceNode> nodes = new ArrayList<IReferenceNode>();
                if (foundSourceReferences.size() > 0) {
                    for (Map.Entry<IAspectDefinition, List<IAopReference>> entry : foundSourceReferences
                            .entrySet()) {
                        nodes.add(new AdviceRootAopReferenceNode(entry
                                .getValue()));
                    }
                }
                if (foundTargetReferences.size() > 0) {
                    for (Map.Entry<IBean, List<IAopReference>> entry : foundTargetReferences
                            .entrySet()) {
                        nodes.add(new AdvisedRootAopReferenceNode(entry
                                .getValue()));
                    }
                }
                return nodes.toArray();
            }
        }
        else if (parentElement instanceof ElementImpl) {
            ElementImpl element = (ElementImpl) parentElement;
            IStructuredDocument document = element.getStructuredDocument();
            int startLine = document.getLineOfOffset(element.getStartOffset()) + 1;
            int endLine = document.getLineOfOffset(element.getEndOffset()) + 1;
            IResource resource = getResource(document);
            IAopProject project = Activator.getModel().getProject(
                    resource.getProject());
            List<IAopReference> references = project
                    .getReferencesForResource(resource);
            Map<IAspectDefinition, List<IAopReference>> foundSourceReferences = new HashMap<IAspectDefinition, List<IAopReference>>();
            Map<IBean, List<IAopReference>> foundTargetReferences = new HashMap<IBean, List<IAopReference>>();
            for (IAopReference reference : references) {
                if (reference.getDefinition().getAspectLineNumber() >= startLine
                        && reference.getDefinition().getAspectLineNumber() <= endLine) {
                    if (foundSourceReferences.containsKey(reference
                            .getDefinition())) {
                        foundSourceReferences.get(reference.getDefinition())
                                .add(reference);
                    }
                    else {
                        List<IAopReference> tmp = new ArrayList<IAopReference>();
                        tmp.add(reference);
                        foundSourceReferences.put(reference.getDefinition(),
                                tmp);
                    }
                }
                if (reference.getTargetBean().getElementStartLine() >= startLine
                        && reference.getTargetBean().getElementEndLine() <= endLine) {
                    if (foundTargetReferences.containsKey(reference
                            .getTargetBean())) {
                        foundTargetReferences.get(reference.getTargetBean())
                                .add(reference);
                    }
                    else {
                        List<IAopReference> tmp = new ArrayList<IAopReference>();
                        tmp.add(reference);
                        foundTargetReferences.put(reference.getTargetBean(),
                                tmp);
                    }
                }
            }
            List<IReferenceNode> nodes = new ArrayList<IReferenceNode>();
            if (foundSourceReferences.size() > 0) {
                for (Map.Entry<IAspectDefinition, List<IAopReference>> entry : foundSourceReferences
                        .entrySet()) {
                    nodes.add(new AdviceRootAopReferenceNode(entry.getValue(),
                            true));
                }
            }
            if (foundTargetReferences.size() > 0) {
                for (Map.Entry<IBean, List<IAopReference>> entry : foundTargetReferences
                        .entrySet()) {
                    nodes.add(new AdvisedRootAopReferenceNode(entry.getValue(),
                            true));
                }
            }
            // add normal beans
            IBeansConfig beansConfig = BeansCorePlugin.getModel().getProject(
                    resource.getProject()).getConfig((IFile) resource);
            Set<IBean> beans = beansConfig.getBeans();
            for (IBean bean : beans) {
                if (!foundTargetReferences.containsKey(bean)
                        && bean.getElementStartLine() >= startLine
                        && bean.getElementEndLine() <= endLine) {
                    nodes.add(new BeanReferenceNode(bean));
                }
            }
            return nodes.toArray();
        }
        return IModelElement.NO_CHILDREN;
    }

    private IResource getResource(IStructuredDocument document) {
        IStructuredModel model = StructuredModelManager.getModelManager()
                .getExistingModelForEdit(document);
        String baselocation = model.getBaseLocation();
        IResource resource = null;
        if (baselocation != null) {
            // copied from JSPTranslationAdapter#getJavaProject
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath filePath = new Path(baselocation);
            if (filePath.segmentCount() > 0) {
                resource = root.getFile(filePath);
            }
        }
        return resource;
    }

    public Object getParent(Object element) {
        if (element instanceof IModelElement) {
            return ((IModelElement) element).getElementParent();
        }
        else if (element instanceof IFile) {
            return BeansCorePlugin.getModel().getConfig((IFile) element)
                    .getElementParent();
        }
        if (element instanceof ZipEntryStorage) {
            return BeansCorePlugin.getModel().getConfig(
                    ((ZipEntryStorage) element).getFullName())
                    .getElementParent();
        }
        return null;
    }

    @SuppressWarnings("restriction")
    public boolean hasChildren(Object element) {
        if (element instanceof IReferenceNode) {
            return ((IReferenceNode) element).hasChildren();
        }
        else if (element instanceof IType) {
            IType type = (IType) element;
            try {
                IMethod[] methods = type.getMethods();
                for (IMethod method : methods) {
                    if (Activator.getModel().isAdvised(method)
                            || Activator.getModel().isAdvice(method)) {
                        return true;
                    }
                }
            }
            catch (JavaModelException e) {
            }
        }
        else if (element instanceof IMethod && element instanceof SourceMethod) {
            IMethod method = (IMethod) element;
            IAopProject project = Activator.getModel().getProject(
                    method.getJavaProject().getProject());
            if (project != null && project.getAllReferences().size() > 0) {
                List<IAopReference> references = project.getAllReferences();
                for (IAopReference reference : references) {
                    if (reference.getTarget().equals(method)) {
                        return true;
                    }
                    if (reference.getSource().equals(method)) {
                        return true;
                    }
                }
            }
            else {
                return false;
            }
        }
        return false;
    }

    public void elementChanged(ModelChangeEvent event) {
        IModelElement element = event.getElement();
        if (element instanceof IBeansConfig) {
            IBeansConfig config = (IBeansConfig) element;
            Set<String> classes = config.getBeanClasses();
            for (String clz : classes) {
                IType type = BeansModelUtils.getJavaType(config
                        .getElementResource().getProject(), clz);
                if (type != null && type instanceof SourceType) {
                    refreshViewer(type);
                }
            }
        }
    }

    protected void refreshViewer(final Object element) {
        if (viewer instanceof TreeViewer) {
            Control ctrl = viewer.getControl();

            // Are we in the UI thread?
            if (ctrl.getDisplay().getThread() == Thread.currentThread()) {
                BeansAopNavigator.refreshViewer((TreeViewer) viewer, element);
            }
            else {
                ctrl.getDisplay().asyncExec(new Runnable() {
                    public void run() {

                        // Abort if this happens after disposes
                        Control ctrl = viewer.getControl();
                        if (ctrl == null || ctrl.isDisposed()) {
                            return;
                        }
                        BeansAopNavigator.refreshViewer((TreeViewer) viewer, element);
                    }
                });
            }
        }
    }

    public void init(ICommonContentExtensionSite config) {
        contentExtension = config.getExtension();
    }

    public void saveState(IMemento aMemento) {
    }

    public void restoreState(IMemento aMemento) {
    }
}

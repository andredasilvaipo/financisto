/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto2.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto2.R;
import ru.orangesoftware.financisto2.db.CategoryRepository;
import ru.orangesoftware.financisto2.db.CategoryRepository_;
import ru.orangesoftware.financisto2.db.DatabaseAdapter;
import ru.orangesoftware.financisto2.db.DatabaseAdapter_;
import ru.orangesoftware.financisto2.db.DatabaseHelper;
import ru.orangesoftware.financisto2.model.Attribute;
import ru.orangesoftware.financisto2.model.Category;
import ru.orangesoftware.financisto2.model.MyEntity;
import ru.orangesoftware.financisto2.model.Transaction;
import ru.orangesoftware.financisto2.model.TransactionAttribute;
import ru.orangesoftware.financisto2.utils.TransactionUtils;
import ru.orangesoftware.financisto2.view.AttributeView;
import ru.orangesoftware.financisto2.view.AttributeViewFactory;

public class CategorySelector {

    private final Activity activity;
    private final DatabaseAdapter db;
    private final CategoryRepository categoryRepository;
    private final ActivityLayout x;

    private TextView categoryText;
    private List<Category> categories;
    private ListAdapter categoryAdapter;
    private LinearLayout attributesLayout;

    private Category selectedCategory;
    private CategorySelectorListener listener;
    private boolean showSplitCategory = true;
    boolean fetchAllCategories = true;

    public CategorySelector(Activity activity, ActivityLayout x, boolean fetchAllCategories) {
        this.activity = activity;
        this.db = DatabaseAdapter_.getInstance_(activity);
        this.categoryRepository = CategoryRepository_.getInstance_(activity);
        this.x = x;
        this.fetchAllCategories = fetchAllCategories;
        this.selectedCategory = Category.noCategory(activity);
    }

    public void setListener(CategorySelectorListener listener) {
        this.listener = listener;
    }

    public void doNotShowSplitCategory() {
        this.showSplitCategory = false;
    }

    public void fetchCategories() {
        categories = categoryRepository.loadCategories().asFlatList();
        categories.add(0, Category.noCategory(activity));
        if (fetchAllCategories) {
            categories.add(0, Category.splitCategory(activity));
        }
        categoryAdapter = TransactionUtils.createCategoryAdapter(db, activity, categories);
    }

    public void createNode(LinearLayout layout, boolean showSplitButton) {
        if (showSplitButton) {
            categoryText = x.addListNodeCategory(layout);
        } else {
            categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
        }
        categoryText.setText(R.string.no_category);
    }

    public void createDummyNode() {
        categoryText = new EditText(activity);
    }

    public void onClick(int id) {
        switch (id) {
            case R.id.category: {
                if (!CategorySelectorActivity.pickCategory(activity, selectedCategory.id, showSplitCategory)) {
                    int selectedPosition = MyEntity.indexOf(categories, selectedCategory.id);
                    x.selectItemId(activity, R.id.category, R.string.category, categoryAdapter, selectedPosition);
                }
                break;
            }
            case R.id.category_add: {
                CategoryActivity_.intent(activity).startForResult(R.id.category_add);
                break;
            }
            case R.id.category_split:
                selectCategory(Category.SPLIT_CATEGORY_ID);
                break;
        }
    }

    public void onSelectedId(int id, long selectedId) {
        if (id == R.id.category) {
            selectCategory(selectedId);
        }
    }

    public long getSelectedCategoryId() {
        return selectedCategory.id;
    }

    public void selectCategory(long categoryId) {
        selectCategory(categoryId, true);
    }

    public void selectCategory(long categoryId, boolean selectLast) {
        if (selectedCategory.id != categoryId) {
            Category category = categoryRepository.getCategoryById(categoryId);
            if (category != null) {
                categoryText.setText(Category.getTitle(category.title, category.level));
                selectedCategory = category;
                if (listener != null) {
                    listener.onCategorySelected(category, selectLast);
                }
            }
        }
    }

    public void createAttributesLayout(LinearLayout layout) {
        attributesLayout = new LinearLayout(activity);
        attributesLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(attributesLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    protected List<TransactionAttribute> getAttributes() {
        List<TransactionAttribute> list = new LinkedList<TransactionAttribute>();
        long count = attributesLayout.getChildCount();
        for (int i=0; i<count; i++) {
            View v = attributesLayout.getChildAt(i);
            Object o = v.getTag();
            if (o instanceof AttributeView) {
                AttributeView av = (AttributeView)o;
                TransactionAttribute ta = av.newTransactionAttribute();
                list.add(ta);
            }
        }
        return list;
    }

    public void addAttributes(Transaction transaction) {
        attributesLayout.removeAllViews();
        List<Attribute> attributes = db.getAllAttributesForCategory(selectedCategory);
        Map<Long, String> values = transaction.categoryAttributes;
        for (Attribute a : attributes) {
            AttributeView av = inflateAttribute(a);
            String value = values != null ? values.get(a.id) : null;
            if (value == null) {
                value = a.defaultValue;
            }
            View v = av.inflateView(attributesLayout, value);
            v.setTag(av);
        }
    }

    private AttributeView inflateAttribute(Attribute attribute) {
        return AttributeViewFactory.createViewForAttribute(activity, attribute);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case R.id.category_add: {
                    fetchCategories();
                    long categoryId = data.getLongExtra(DatabaseHelper.CategoryColumns._id.name(), -1);
                    if (categoryId != -1) {
                        selectCategory(categoryId);
                    }
                    break;
                }
                case R.id.category_pick: {
                    long categoryId = data.getLongExtra(CategorySelectorActivity.SELECTED_CATEGORY_ID, 0);
                    selectCategory(categoryId);
                    break;
                }
            }
        }
    }

    public boolean isSplitCategorySelected() {
        return selectedCategory.isSplit();
    }

    public interface CategorySelectorListener {
        void onCategorySelected(Category category, boolean selectLast);
    }

}

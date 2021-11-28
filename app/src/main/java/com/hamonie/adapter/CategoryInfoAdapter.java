package com.hamonie.adapter;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

import com.hamonie.appthemehelper.ThemeStore;
import com.hamonie.R;
import com.hamonie.model.CategoryInfo;
import com.hamonie.util.SwipeAndDragHelper;

public class CategoryInfoAdapter extends RecyclerView.Adapter<CategoryInfoAdapter.ViewHolder>
    implements SwipeAndDragHelper.ActionCompletionContract {

  private List<CategoryInfo> categoryInfos;
  private final ItemTouchHelper touchHelper;

  public CategoryInfoAdapter() {
    SwipeAndDragHelper swipeAndDragHelper = new SwipeAndDragHelper(this);
    touchHelper = new ItemTouchHelper(swipeAndDragHelper);
  }

  public void attachToRecyclerView(RecyclerView recyclerView) {
    touchHelper.attachToRecyclerView(recyclerView);
  }

  @NonNull
  public List<CategoryInfo> getCategoryInfos() {
    return categoryInfos;
  }

  public void setCategoryInfos(@NonNull List<CategoryInfo> categoryInfos) {
    this.categoryInfos = categoryInfos;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return categoryInfos.size();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull CategoryInfoAdapter.ViewHolder holder, int position) {
    CategoryInfo categoryInfo = categoryInfos.get(position);

    holder.checkBox.setChecked(categoryInfo.isVisible());
    holder.title.setText(
        holder.title.getResources().getString(categoryInfo.getCategory().getStringRes()));

    holder.itemView.setOnClickListener(
        v -> {
          if (!(categoryInfo.isVisible() && isLastCheckedCategory(categoryInfo))) {
            categoryInfo.setVisible(!categoryInfo.isVisible());
            holder.checkBox.setChecked(categoryInfo.isVisible());
          } else {
            Toast.makeText(
                    holder.itemView.getContext(),
                    R.string.you_have_to_select_at_least_one_category,
                    Toast.LENGTH_SHORT)
                .show();
          }
        });

    holder.dragView.setOnTouchListener(
        (view, event) -> {
          if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchHelper.startDrag(holder);
          }
          return false;
        });
  }

  @Override
  @NonNull
  public CategoryInfoAdapter.ViewHolder onCreateViewHolder(
      @NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.preference_dialog_library_categories_listitem, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onViewMoved(int oldPosition, int newPosition) {
    CategoryInfo categoryInfo = categoryInfos.get(oldPosition);
    categoryInfos.remove(oldPosition);
    categoryInfos.add(newPosition, categoryInfo);
    notifyItemMoved(oldPosition, newPosition);
  }

  private boolean isLastCheckedCategory(CategoryInfo categoryInfo) {
    if (categoryInfo.isVisible()) {
      for (CategoryInfo c : categoryInfos) {
        if (c != categoryInfo && c.isVisible()) {
          return false;
        }
      }
    }
    return true;
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    private final MaterialCheckBox checkBox;
    private final View dragView;
    private final TextView title;

    ViewHolder(View view) {
      super(view);
      checkBox = view.findViewById(R.id.checkbox);
      checkBox.setButtonTintList(
              ColorStateList.valueOf(ThemeStore.Companion.accentColor(checkBox.getContext())));
      title = view.findViewById(R.id.title);
      dragView = view.findViewById(R.id.drag_view);
    }
  }
}

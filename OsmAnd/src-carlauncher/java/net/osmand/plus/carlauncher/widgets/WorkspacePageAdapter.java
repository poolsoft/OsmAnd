package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager2 icinde her bir 4x4 grid sayfasini dolduran Adapter sinifi.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class WorkspacePageAdapter extends RecyclerView.Adapter<WorkspacePageAdapter.PageViewHolder> {

    private final Context context;
    private final FragmentManager fragmentManager;
    private final int pageCount = 3; // Toplam masaustu sayfa sayisi
    private final List<BaseWidget> widgetsList;
    private final Runnable onWidgetsChangedListener;

    public WorkspacePageAdapter(Context context, FragmentManager fragmentManager, List<BaseWidget> widgets, Runnable onWidgetsChangedListener) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.widgetsList = widgets;
        this.onWidgetsChangedListener = onWidgetsChangedListener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.workspace_page_grid, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return pageCount;
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        GridLayout gridLayout;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            gridLayout = itemView.findViewById(R.id.workspace_grid);
        }

        public void bind(int pageIndex) {
            gridLayout.removeAllViews();
            
            // Bu sayfadaki gorunur widget'lari bul
            List<BaseWidget> pageWidgets = new ArrayList<>();
            for (BaseWidget widget : widgetsList) {
                if (widget.isVisible() && widget.getPageIndex() == pageIndex) {
                    pageWidgets.add(widget);
                }
            }

            // Izgara doluluk matrisi (4x4)
            boolean[][] occupied = new boolean[4][4];

            // Once koordinati belli olan widget'lari yerlestirelim ve matrisi isaretleyelim
            for (BaseWidget widget : pageWidgets) {
                int cx = widget.getCellX();
                int cy = widget.getCellY();
                int sx = widget.getSpanX();
                int sy = widget.getSpanY();

                if (cx >= 0 && cx + sx <= 4 && cy >= 0 && cy + sy <= 4) {
                    for (int x = cx; x < cx + sx; x++) {
                        for (int y = cy; y < cy + sy; y++) {
                            occupied[x][y] = true;
                        }
                    }
                }
            }

            // Şimdi koordinati olmayan widget'lara bos yer bulup atayalim
            boolean needsSave = false;
            for (BaseWidget widget : pageWidgets) {
                if (widget.getCellX() == -1 || widget.getCellY() == -1) {
                    int sx = widget.getSpanX();
                    int sy = widget.getSpanY();
                    Point emptyPoint = findEmptySpace(occupied, sx, sy);
                    if (emptyPoint != null) {
                        widget.setCellX(emptyPoint.x);
                        widget.setCellY(emptyPoint.y);
                        for (int x = emptyPoint.x; x < emptyPoint.x + sx; x++) {
                            for (int y = emptyPoint.y; y < emptyPoint.y + sy; y++) {
                                occupied[x][y] = true;
                            }
                        }
                        needsSave = true;
                    } else {
                        // Sayfa doluysa, bir sonraki sayfaya tasi
                        if (pageIndex < pageCount - 1) {
                            widget.setPageIndex(pageIndex + 1);
                            widget.setCellX(-1);
                            widget.setCellY(-1);
                            needsSave = true;
                        }
                    }
                }
            }

            if (needsSave) {
                WidgetManager.getInstance(context).saveWidgetConfig();
                gridLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        if (onWidgetsChangedListener != null) {
                            onWidgetsChangedListener.run();
                        }
                    }
                });
            }

            // Sayfadaki tum widget'lari GridLayout'a ekleyelim
            for (final BaseWidget widget : pageWidgets) {
                if (widget.getCellX() == -1 || widget.getCellY() == -1) {
                    continue; // Yer bulunamadiysa ekleme
                }

                View widgetView = widget.getRootView();
                if (widgetView == null) {
                    widgetView = widget.createView();
                }

                if (widgetView != null) {
                    // View parent baglantisini temizle
                    ViewGroup parent = (ViewGroup) widgetView.getParent();
                    if (parent != null) {
                        parent.removeView(widgetView);
                    }

                    // Izgara boyut parametreleri
                    GridLayout.Spec rowSpec = GridLayout.spec(widget.getCellY(), widget.getSpanY(), GridLayout.FILL, 1f);
                    GridLayout.Spec colSpec = GridLayout.spec(widget.getCellX(), widget.getSpanX(), GridLayout.FILL, 1f);
                    
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                    params.width = 0;
                    params.height = 0;
                    
                    // Margins
                    int margin = dpToPx(6);
                    params.setMargins(margin, margin, margin, margin);
                    widgetView.setLayoutParams(params);

                    // Uzun basma olayi (Premium Duzenleme Menusu)
                    widgetView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            showWidgetOptionsPopupMenu(v, widget);
                            return true;
                        }
                    });

                    gridLayout.addView(widgetView);
                }
            }
        }

        private Point findEmptySpace(boolean[][] occupied, int spanX, int spanY) {
            for (int y = 0; y <= 4 - spanY; y++) {
                for (int x = 0; x <= 4 - spanX; x++) {
                    boolean fits = true;
                    for (int sy = 0; sy < spanY; sy++) {
                        for (int sx = 0; sx < spanX; sx++) {
                            if (occupied[x + sx][y + sy]) {
                                fits = false;
                                break;
                            }
                        }
                        if (!fits) break;
                    }
                    if (fits) {
                        return new Point(x, y);
                    }
                }
            }
            return null;
        }

        private void showWidgetOptionsPopupMenu(View anchorView, final BaseWidget widget) {
            PopupMenu popup = new PopupMenu(context, anchorView);
            
            // Menu maddelerini dinamik ekleyelim
            if (widget.isConfigurable()) {
                popup.getMenu().add(0, 1, 0, "Widget Ayarlari");
            }
            
            popup.getMenu().add(0, 2, 1, "Boyut: 1x1 (Kucuk)");
            popup.getMenu().add(0, 3, 2, "Boyut: 2x1 (Orta)");
            popup.getMenu().add(0, 4, 3, "Boyut: 2x2 (Buyuk)");
            
            // Sayfa tasima secenekleri
            if (widget.getPageIndex() > 0) {
                popup.getMenu().add(0, 5, 4, "Sola Tasi (Sayfa " + widget.getPageIndex() + ")");
            }
            if (widget.getPageIndex() < pageCount - 1) {
                popup.getMenu().add(0, 6, 5, "Saga Tasi (Sayfa " + (widget.getPageIndex() + 2) + ")");
            }
            
            popup.getMenu().add(0, 7, 6, "Kapat / Kaldir");

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    int itemId = item.getItemId();
                    if (itemId == 1) {
                        widget.openConfig(fragmentManager);
                        return true;
                    } else if (itemId == 2) {
                        changeWidgetSize(widget, BaseWidget.WidgetSize.SMALL);
                        return true;
                    } else if (itemId == 3) {
                        changeWidgetSize(widget, BaseWidget.WidgetSize.MEDIUM);
                        return true;
                    } else if (itemId == 4) {
                        changeWidgetSize(widget, BaseWidget.WidgetSize.LARGE);
                        return true;
                    } else if (itemId == 5) {
                        moveWidgetToPage(widget, widget.getPageIndex() - 1);
                        return true;
                    } else if (itemId == 6) {
                        moveWidgetToPage(widget, widget.getPageIndex() + 1);
                        return true;
                    } else if (itemId == 7) {
                        removeWidgetFromWorkspace(widget);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        }

        private void changeWidgetSize(BaseWidget widget, BaseWidget.WidgetSize newSize) {
            widget.setSize(newSize);
            // Boyut degistikten sonra koordinatlari sifirlayalim ki bos yere otomatik yerlessin
            widget.setCellX(-1);
            widget.setCellY(-1);
            WidgetManager.getInstance(context).saveWidgetConfig();
            notifyDataSetChanged();
            if (onWidgetsChangedListener != null) {
                onWidgetsChangedListener.run();
            }
            Toast.makeText(context, widget.getTitle() + " boyutu guncellendi.", Toast.LENGTH_SHORT).show();
        }

        private void moveWidgetToPage(BaseWidget widget, int targetPage) {
            widget.setPageIndex(targetPage);
            widget.setCellX(-1);
            widget.setCellY(-1);
            WidgetManager.getInstance(context).saveWidgetConfig();
            notifyDataSetChanged();
            if (onWidgetsChangedListener != null) {
                onWidgetsChangedListener.run();
            }
        }

        private void removeWidgetFromWorkspace(BaseWidget widget) {
            widget.setVisible(false);
            WidgetManager.getInstance(context).saveWidgetConfig();
            notifyDataSetChanged();
            if (onWidgetsChangedListener != null) {
                onWidgetsChangedListener.run();
            }
            Toast.makeText(context, widget.getTitle() + " kaldirildi.", Toast.LENGTH_SHORT).show();
        }

        private int dpToPx(int dp) {
            float density = context.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}

package cz.gvid.app;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

@SuppressWarnings({"IfCanBeSwitch", "WeakerAccess"})
public class CustomList extends BaseAdapter {

    private LayoutInflater inflater;
    private Context appContext;
    private int ListViewLayoutId;

    public CustomList(Context appContext, int ListViewLayout) {
        inflater = (LayoutInflater)appContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.appContext = appContext;
        ListViewLayoutId = ListViewLayout;
    }

    private ArrayList<String> itemType = new ArrayList<>();
    private ArrayList<String> itemParam1 = new ArrayList<>();
    private ArrayList<String> itemParam2 = new ArrayList<>();
    private ArrayList<String> itemParam3 = new ArrayList<>();
    private ArrayList<String> itemParam4 = new ArrayList<>();
    private ArrayList<String> itemParam5 = new ArrayList<>();

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
        //Here can be changed recycledView
    }

    @Override
    public View getView(final int position, View recycledView, ViewGroup parent) {
        if (recycledView == null) {
            recycledView = inflater.inflate(ListViewLayoutId, parent, false);
        }
        View rowView = recycledView;

        if(ListViewLayoutId == R.layout.listview_aktuality) {
            LinearLayout LV_row_article = rowView.findViewById(R.id.row_article);
            LinearLayout LV_row_gvid = rowView.findViewById(R.id.row_gvid);
            LinearLayout LV_row_moodle = rowView.findViewById(R.id.row_moodle);
            LinearLayout LV_row_noarticle = rowView.findViewById(R.id.row_noarticle);
            LV_row_article.setVisibility(View.GONE);
            LV_row_gvid.setVisibility(View.GONE);
            LV_row_moodle.setVisibility(View.GONE);
            LV_row_noarticle.setVisibility(View.GONE);

            TextView TV_label_date = rowView.findViewById(R.id.label_date);
            TextView TV_label_month = rowView.findViewById(R.id.label_month);
            TextView TV_label_title = rowView.findViewById(R.id.label_title);

            String ItemType = itemType.get(position);
            if(ItemType.equals("article")) {
                LV_row_article.setVisibility(View.VISIBLE);
                TV_label_date.setText(itemParam1.get(position));
                TV_label_month.setText(itemParam2.get(position));
                TV_label_title.setText(itemParam3.get(position));
            } else if(ItemType.equals("gvid")) {
                LV_row_gvid.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("moodle")) {
                LV_row_moodle.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("noarticle")) {
                LV_row_noarticle.setVisibility(View.VISIBLE);
            }
        } else if(ListViewLayoutId == R.layout.listview_suplovani) {
            LinearLayout LV_row_suply = rowView.findViewById(R.id.row_suply);
            LinearLayout LV_row_suply_old = rowView.findViewById(R.id.row_suply_old);
            LinearLayout LV_row_suply_file = rowView.findViewById(R.id.row_suply_file);
            LinearLayout LV_row_suply_nofile = rowView.findViewById(R.id.row_suply_nofile);
            LV_row_suply.setVisibility(View.GONE);
            LV_row_suply_old.setVisibility(View.GONE);
            LV_row_suply_file.setVisibility(View.GONE);
            LV_row_suply_nofile.setVisibility(View.GONE);

            TextView TV_label_suply_title = rowView.findViewById(R.id.label_suply_title);
            TextView TV_label_suply_pdf_file = rowView.findViewById(R.id.label_suply_pdf_file);

            String ItemType = itemType.get(position);
            if(ItemType.equals("file") || ItemType.equals("fileold")) {
                LV_row_suply_file.setVisibility(View.VISIBLE);
                TV_label_suply_title.setText(itemParam1.get(position));
                TV_label_suply_pdf_file.setText(itemParam2.get(position));
            } else if(ItemType.equals("latest")) {
                LV_row_suply.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("old")) {
                LV_row_suply_old.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("nofile")) {
                LV_row_suply_nofile.setVisibility(View.VISIBLE);
            }
        } else if(ListViewLayoutId == R.layout.listview_znamky) {
            LinearLayout LV_row_zahlavi = rowView.findViewById(R.id.row_zahlavi);
            LinearLayout LV_row_znamka = rowView.findViewById(R.id.row_znamka);
            LinearLayout LV_row_znamka_2 = rowView.findViewById(R.id.row_znamka_2);
            LinearLayout LV_row_znamka_prumer = rowView.findViewById(R.id.row_znamka_prumer);
            LinearLayout LV_row_znamka_2_prumer = rowView.findViewById(R.id.row_znamka_2_prumer);
            LinearLayout LV_row_zahlavi_hodnoceni = rowView.findViewById(R.id.row_zahlavi_hodnoceni);
            LinearLayout LV_row_hodnoceni = rowView.findViewById(R.id.row_hodnoceni);
            LinearLayout LV_row_hodnoceni_2 = rowView.findViewById(R.id.row_hodnoceni_2);
            LV_row_zahlavi.setVisibility(View.GONE);
            LV_row_znamka.setVisibility(View.GONE);
            LV_row_znamka_2.setVisibility(View.GONE);
            LV_row_znamka_prumer.setVisibility(View.GONE);
            LV_row_znamka_2_prumer.setVisibility(View.GONE);
            LV_row_zahlavi_hodnoceni.setVisibility(View.GONE);
            LV_row_hodnoceni.setVisibility(View.GONE);
            LV_row_hodnoceni_2.setVisibility(View.GONE);

            String ItemType = itemType.get(position);
            if(ItemType.equals("zahlavi")) {
                LV_row_zahlavi.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("znamka")) {
                if(position % 2 == 0) {
                    ((TextView) rowView.findViewById(R.id.row_znamka_datum)).setText(itemParam1.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_predmet)).setText(itemParam2.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_znamka)).setText(itemParam3.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_vaha)).setText(itemParam4.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_popis)).setText(itemParam5.get(position));
                    LV_row_znamka.setVisibility(View.VISIBLE);
                } else {
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_datum)).setText(itemParam1.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_predmet)).setText(itemParam2.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_znamka)).setText(itemParam3.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_vaha)).setText(itemParam4.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_popis)).setText(itemParam5.get(position));
                    LV_row_znamka_2.setVisibility(View.VISIBLE);
                }
            } else if(ItemType.equals("prumer")) {
                if(position % 2 == 0) {
                    ((TextView) rowView.findViewById(R.id.row_znamka_prumer_predmet)).setText(itemParam2.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_prumer_prumer)).setText(itemParam1.get(position));
                    LV_row_znamka_prumer.setVisibility(View.VISIBLE);
                } else {
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_prumer_predmet)).setText(itemParam2.get(position));
                    ((TextView) rowView.findViewById(R.id.row_znamka_2_prumer_prumer)).setText(itemParam1.get(position));
                    LV_row_znamka_2_prumer.setVisibility(View.VISIBLE);
                }
            } else if(ItemType.equals("zahlavi_hodnoceni")) {
                LV_row_zahlavi_hodnoceni.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("znamka_hodnoceni")) {
                if(position % 2 == 0) {
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_2_predmet)).setText(itemParam1.get(position));
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_2_prumer)).setText(itemParam2.get(position));
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_2_ctvrtleti)).setText(itemParam3.get(position));
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_2_pololeti)).setText(itemParam4.get(position));
                    LV_row_hodnoceni_2.setVisibility(View.VISIBLE);
                } else {
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_predmet)).setText(itemParam1.get(position));
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_prumer)).setText(itemParam2.get(position));
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_ctvrtleti)).setText(itemParam3.get(position));
                    ((TextView) rowView.findViewById(R.id.row_hodnoceni_pololeti)).setText(itemParam4.get(position));
                    LV_row_hodnoceni.setVisibility(View.VISIBLE);
                }
            }
        } else if(ListViewLayoutId == R.layout.listview_obedy) {
            View LV_row_obedTop = rowView.findViewById(R.id.obedTop);
            LinearLayout LV_row_obedNormal = rowView.findViewById(R.id.obedNormal);
            LinearLayout LV_row_obedSelected = rowView.findViewById(R.id.obedSelected);
            LinearLayout LV_row_obedDay = rowView.findViewById(R.id.obedDay);
            View LV_row_obedBottom = rowView.findViewById(R.id.obedBottom);
            LV_row_obedTop.setVisibility(View.GONE);
            LV_row_obedNormal.setVisibility(View.GONE);
            LV_row_obedSelected.setVisibility(View.GONE);
            LV_row_obedDay.setVisibility(View.GONE);
            LV_row_obedBottom.setVisibility(View.GONE);

            TextView TV_obedNormalName = rowView.findViewById(R.id.obedNormalName);
            TextView TV_obedNormalType = rowView.findViewById(R.id.obedNormalType);
            TextView TV_obedSelectedName = rowView.findViewById(R.id.obedSelectedName);
            TextView TV_obedSelectedType = rowView.findViewById(R.id.obedSelectedType);
            TextView TV_obedDayDay = rowView.findViewById(R.id.obedDayDay);
            TextView TV_obedDayDate = rowView.findViewById(R.id.obedDayDate);

            String ItemType = itemType.get(position);
            if(ItemType.equals("lunch")) {
                LV_row_obedNormal.setVisibility(View.VISIBLE);
                TV_obedNormalName.setText(itemParam2.get(position));
                TV_obedNormalType.setText(itemParam1.get(position));
            } else if(ItemType.equals("lunchselected")) {
                LV_row_obedSelected.setVisibility(View.VISIBLE);
                TV_obedSelectedName.setText(itemParam2.get(position));
                TV_obedSelectedType.setText(itemParam1.get(position));
            } else if(ItemType.equals("lunchdate")) {
                LV_row_obedDay.setVisibility(View.VISIBLE);
                TV_obedDayDay.setText(itemParam1.get(position));
                TV_obedDayDate.setText(itemParam2.get(position));
            } else if(ItemType.equals("top")) {
                LV_row_obedTop.setVisibility(View.VISIBLE);
            } else if(ItemType.equals("bottom")) {
                LV_row_obedBottom.setVisibility(View.VISIBLE);
            }
        } else if(ListViewLayoutId == R.layout.listview_objednavani) {
            LinearLayout LV_row_obedDay = rowView.findViewById(R.id.obedDay);
            LinearLayout LV_row_obedZakazano = rowView.findViewById(R.id.obedZakazano);
            LinearLayout LV_row_obedZakazanoObjednano =  rowView.findViewById(R.id.obedZakazanoObjednano);
            LinearLayout LV_row_obedObjednano = rowView.findViewById(R.id.obedObjednano);
            LinearLayout LV_row_obedObjednat = rowView.findViewById(R.id.obedObjednat);
            TextView LV_row_obedPopis = rowView.findViewById(R.id.obedPopis);
            LV_row_obedDay.setVisibility(View.GONE);
            LV_row_obedZakazano.setVisibility(View.GONE);
            LV_row_obedZakazanoObjednano.setVisibility(View.GONE);
            LV_row_obedObjednano.setVisibility(View.GONE);
            LV_row_obedObjednat.setVisibility(View.GONE);
            LV_row_obedPopis.setVisibility(View.GONE);

            TextView TV_obedDayDay = rowView.findViewById(R.id.obedDayDay);
            TextView TV_obedDayDate = rowView.findViewById(R.id.obedDayDate);
            TextView TV_obedZakazanoType = rowView.findViewById(R.id.obedZakazanoType);
            TextView TV_obedZakazanoStatus = rowView.findViewById(R.id.obedZakazanoStatus);
            TextView TV_obedZakazanoObjednanoType = rowView.findViewById(R.id.obedZakazanoObjednanoType);
            TextView TV_obedZakazanoObjednanoStatus = rowView.findViewById(R.id.obedZakazanoObjednanoStatus);
            TextView TV_obedObjednanoType = rowView.findViewById(R.id.obedObjednanoType);
            TextView TV_obedObjednanoStatus = rowView.findViewById(R.id.obedObjednanoStatus);
            TextView TV_obedObjednatType = rowView.findViewById(R.id.obedObjednatType);
            TextView TV_obedObjednatStatus = rowView.findViewById(R.id.obedObjednatStatus);

            String ItemType = itemType.get(position);
            if(ItemType.equals("day")) {
                LV_row_obedDay.setVisibility(View.VISIBLE);
                TV_obedDayDay.setText(itemParam1.get(position));
                TV_obedDayDate.setText(itemParam2.get(position));
            } else if(ItemType.equals("item")) {
                String LunchStatus = itemParam2.get(position);
                if(LunchStatus.equals("nelze objednat")) {
                    LV_row_obedZakazano.setVisibility(View.VISIBLE);
                    TV_obedZakazanoType.setText(itemParam1.get(position));
                    TV_obedZakazanoStatus.setText(itemParam2.get(position));
                } else if(LunchStatus.equals("nelze zrušit")) {
                    LV_row_obedZakazanoObjednano.setVisibility(View.VISIBLE);
                    TV_obedZakazanoObjednanoType.setText(itemParam1.get(position));
                    TV_obedZakazanoObjednanoStatus.setText(itemParam2.get(position));
                } else if(LunchStatus.equals("zrušit")) {
                    LV_row_obedObjednano.setVisibility(View.VISIBLE);
                    TV_obedObjednanoType.setText(itemParam1.get(position));
                    TV_obedObjednanoStatus.setText(itemParam2.get(position));
                } else if(LunchStatus.equals("přeobjednat") || LunchStatus.equals("objednat")) {
                    LV_row_obedObjednat.setVisibility(View.VISIBLE);
                    TV_obedObjednatType.setText(itemParam1.get(position));
                    TV_obedObjednatStatus.setText(itemParam2.get(position));
                }
            } else if(ItemType.equals("text")) {
                LV_row_obedPopis.setVisibility(View.VISIBLE);
                LV_row_obedPopis.setText(itemParam1.get(position));
            }
        }

        return rowView;
    }

    public void addItem(String ItemType, String param1, String param2, String param3, String param4, String param5) {
        itemType.add(ItemType);
        itemParam1.add(fromHTML(param1));
        itemParam2.add(fromHTML(param2));
        itemParam3.add(fromHTML(param3));
        itemParam4.add(fromHTML(param4));
        itemParam5.add(fromHTML(param5));
    }

    public void clear() {
        itemType.clear();
        itemParam1.clear();
        itemParam2.clear();
        itemParam3.clear();
        itemParam4.clear();
        itemParam5.clear();
    }

    public void reload() {
        notifyDataSetChanged();
    }

    public String getParam(int item, int position) {
        if(item == 0) {
            return itemType.get(position);
        } else if(item == 1) {
            return itemParam1.get(position);
        } else if(item == 2) {
            return itemParam2.get(position);
        } else if(item == 3) {
            return itemParam3.get(position);
        } else if(item == 4) {
            return itemParam4.get(position);
        }
        return "";
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return itemType.size();
    }

    private String fromHTML(String htmlString) {
        if (Build.VERSION.SDK_INT >= 24) {
            htmlString = Html.fromHtml(htmlString , Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            htmlString = Html.fromHtml(htmlString).toString();
        }
        return htmlString;
    }
}
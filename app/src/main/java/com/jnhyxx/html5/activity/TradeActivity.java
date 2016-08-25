package com.jnhyxx.html5.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jnhyxx.chart.TrendView;
import com.jnhyxx.chart.domain.TrendViewData;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.order.OrderActivity;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.fragment.PlaceOrderFragment;
import com.jnhyxx.html5.view.BuySellVolumeLayout;
import com.jnhyxx.html5.view.ChartContainer;
import com.jnhyxx.html5.view.TitleBar;
import com.jnhyxx.html5.view.TradePageHeader;
import com.johnz.kutils.DateUtil;
import com.johnz.kutils.Launcher;

import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TradeActivity extends BaseActivity implements PlaceOrderFragment.Callback {

    private static final String TESTDATA = "cu1610,37200.0,20160816090000|cu1610,37190.0,20160816090100|cu1610,37180.00000000001,20160816090200|cu1610,37180.0,20160816090300|cu1610,37190.0,20160816090400|cu1610,37180.0,20160816090500|cu1610,37180.0,20160816090600|cu1610,37220.0,20160816090700|cu1610,37220.0,20160816090800|cu1610,37210.0,20160816090900|cu1610,37200.0,20160816091000|cu1610,37190.0,20160816091100|cu1610,37190.0,20160816091200|cu1610,37190.0,20160816091300|cu1610,37180.0,20160816091400|cu1610,37180.0,20160816091500|cu1610,37170.0,20160816091600|cu1610,37150.0,20160816091700|cu1610,37160.0,20160816091800|cu1610,37130.0,20160816091900|cu1610,37130.0,20160816092000|cu1610,37130.0,20160816092100|cu1610,37140.0,20160816092200|cu1610,37140.0,20160816092300|cu1610,37140.0,20160816092400|cu1610,37150.0,20160816092500|cu1610,37150.0,20160816092600|cu1610,37150.0,20160816092700|cu1610,37160.00000000001,20160816092800|cu1610,37140.0,20160816092900|cu1610,37140.0,20160816093000|cu1610,37130.0,20160816093100|cu1610,37150.0,20160816093200|cu1610,37140.0,20160816093300|cu1610,37130.0,20160816093400|cu1610,37130.0,20160816093500|cu1610,37130.0,20160816093600|cu1610,37130.0,20160816093700|cu1610,37130.0,20160816093800|cu1610,37150.0,20160816093900|cu1610,37150.0,20160816094000|cu1610,37160.0,20160816094100|cu1610,37170.0,20160816094200|cu1610,37160.0,20160816094300|cu1610,37160.0,20160816094400|cu1610,37170.0,20160816094500|cu1610,37160.0,20160816094600|cu1610,37150.0,20160816094700|cu1610,37150.0,20160816094800|cu1610,37150.0,20160816094900|cu1610,37150.0,20160816095000|cu1610,37150.0,20160816095100|cu1610,37140.0,20160816095200|cu1610,37140.0,20160816095300|cu1610,37130.0,20160816095400|cu1610,37130.0,20160816095500|cu1610,37130.0,20160816095600|cu1610,37130.0,20160816095700|cu1610,37120.0,20160816095800|cu1610,37120.00000000001,20160816095900|cu1610,37120.0,20160816100000|cu1610,37130.0,20160816100100|cu1610,37130.0,20160816100200|cu1610,37120.0,20160816100300|cu1610,37110.00000000001,20160816100400|cu1610,37110.00000000001,20160816100500|cu1610,37120.0,20160816100600|cu1610,37130.0,20160816100700|cu1610,37130.0,20160816100800|cu1610,37120.0,20160816100900|cu1610,37120.0,20160816101000|cu1610,37130.0,20160816101100|cu1610,37130.0,20160816101200|cu1610,37120.0,20160816101300|cu1610,37120.0,20160816101400|cu1610,37120.0,20160816103000|cu1610,37120.0,20160816103100|cu1610,37110.0,20160816103200|cu1610,37100.0,20160816103300|cu1610,37100.0,20160816103400|cu1610,37100.0,20160816103500|cu1610,37130.0,20160816103600|cu1610,37110.0,20160816103700|cu1610,37120.00000000001,20160816103800|cu1610,37140.0,20160816103900|cu1610,37140.0,20160816104000|cu1610,37130.0,20160816104100|cu1610,37140.0,20160816104200|cu1610,37130.0,20160816104300|cu1610,37150.0,20160816104400|cu1610,37150.0,20160816104500|cu1610,37150.0,20160816104600|cu1610,37150.0,20160816104700|cu1610,37120.0,20160816104800|cu1610,37120.0,20160816104900|cu1610,37140.0,20160816105000|cu1610,37140.0,20160816105100|cu1610,37140.0,20160816105200|cu1610,37110.0,20160816105300|cu1610,37110.0,20160816105400|cu1610,37120.0,20160816105500|cu1610,37120.0,20160816105600|cu1610,37110.0,20160816105700|cu1610,37100.0,20160816105800|cu1610,37080.0,20160816105900|cu1610,37100.0,20160816110000|cu1610,37100.0,20160816110100|cu1610,37090.0,20160816110200|cu1610,37100.0,20160816110300|cu1610,37110.0,20160816110400|cu1610,37110.00000000001,20160816110500|cu1610,37090.00000000001,20160816110600|cu1610,37080.0,20160816110700|cu1610,37090.00000000001,20160816110800|cu1610,37090.00000000001,20160816110900|cu1610,37090.00000000001,20160816111000|cu1610,37090.00000000001,20160816111100|cu1610,37080.00000000001,20160816111200|cu1610,37090.00000000001,20160816111300|cu1610,37090.00000000001,20160816111400|cu1610,37080.0,20160816111500|cu1610,37080.0,20160816111600|cu1610,37070.0,20160816111700|cu1610,37070.0,20160816111800|cu1610,37070.0,20160816111900|cu1610,37080.0,20160816112000|cu1610,37070.0,20160816112100|cu1610,37070.0,20160816112200|cu1610,37070.0,20160816112300|cu1610,37070.0,20160816112400|cu1610,37080.0,20160816112500|cu1610,37090.0,20160816112600|cu1610,37110.0,20160816112700|cu1610,37090.0,20160816112800|cu1610,37100.0,20160816112900|cu1610,37140.0,20160816133000|cu1610,37150.0,20160816133100|cu1610,37180.0,20160816133200|cu1610,37160.0,20160816133300|cu1610,37170.0,20160816133400|cu1610,37190.0,20160816133500|cu1610,37180.0,20160816133600|cu1610,37180.0,20160816133700|cu1610,37180.0,20160816133800|cu1610,37180.0,20160816133900|cu1610,37190.0,20160816134000|cu1610,37180.0,20160816134100|cu1610,37190.0,20160816134200|cu1610,37190.0,20160816134300|cu1610,37200.00000000001,20160816134400|cu1610,37230.0,20160816134500|cu1610,37220.0,20160816134600|cu1610,37210.0,20160816134700|cu1610,37220.0,20160816134800|cu1610,37220.0,20160816134900|cu1610,37200.0,20160816135000|cu1610,37190.0,20160816135100|cu1610,37190.0,20160816135200|cu1610,37180.0,20160816135300|cu1610,37170.0,20160816135400|cu1610,37180.0,20160816135500|cu1610,37170.0,20160816135600|cu1610,37160.0,20160816135700|cu1610,37170.0,20160816135800|cu1610,37160.0,20160816135900|cu1610,37140.0,20160816140000|cu1610,37130.0,20160816140100|cu1610,37140.0,20160816140200|cu1610,37130.0,20160816140300|cu1610,37130.0,20160816140400|cu1610,37140.0,20160816140500|cu1610,37150.0,20160816140600|cu1610,37160.00000000001,20160816140700|cu1610,37170.0,20160816140800|cu1610,37170.0,20160816140900|cu1610,37160.0,20160816141000|cu1610,37200.0,20160816141100|cu1610,37180.0,20160816141200|cu1610,37170.00000000001,20160816141300|cu1610,37170.0,20160816141400|cu1610,37190.0,20160816141500|cu1610,37190.0,20160816141600|cu1610,37190.0,20160816141700|cu1610,37220.0,20160816141800|cu1610,37210.0,20160816141900|cu1610,37210.00000000001,20160816142000|cu1610,37230.0,20160816142100|cu1610,37230.0,20160816142200|cu1610,37230.0,20160816142300|cu1610,37240.0,20160816142400|cu1610,37230.0,20160816142500|cu1610,37220.0,20160816142600|cu1610,37240.0,20160816142700|cu1610,37260.0,20160816142800|cu1610,37240.0,20160816142900|cu1610,37230.0,20160816143000|cu1610,37230.0,20160816143100|cu1610,37220.0,20160816143200|cu1610,37230.0,20160816143300|cu1610,37240.0,20160816143400|cu1610,37260.00000000001,20160816143500|cu1610,37250.0,20160816143600|cu1610,37250.0,20160816143700|cu1610,37240.0,20160816143800|cu1610,37240.0,20160816143900|cu1610,37230.0,20160816144000|cu1610,37230.00000000001,20160816144100|cu1610,37230.0,20160816144200|cu1610,37250.0,20160816144300|cu1610,37250.0,20160816144400|cu1610,37240.0,20160816144500|cu1610,37240.0,20160816144600|cu1610,37240.0,20160816144700|cu1610,37240.00000000001,20160816144800|cu1610,37240.00000000001,20160816144900|cu1610,37250.0,20160816145000|cu1610,37240.00000000001,20160816145100|cu1610,37230.0,20160816145200|cu1610,37230.00000000001,20160816145300|cu1610,37240.0,20160816145400|cu1610,37220.0,20160816145500|cu1610,37220.0,20160816145600|cu1610,37200.00000000001,20160816145700|cu1610,37220.00000000001,20160816145800|cu1610,37220.0,20160816145900|cu1610,37330.0,20160816205900|cu1610,37310.0,20160816210000|cu1610,37330.0,20160816210100|cu1610,37320.0,20160816210200|cu1610,37350.0,20160816210300|cu1610,37350.0,20160816210400|cu1610,37360.0,20160816210500|cu1610,37340.0,20160816210600|cu1610,37310.0,20160816210700|cu1610,37310.0,20160816210800|cu1610,37310.0,20160816210900|cu1610,37310.0,20160816211000|cu1610,37330.0,20160816211100|cu1610,37310.0,20160816211200|cu1610,37310.0,20160816211300|cu1610,37320.0,20160816211400|cu1610,37270.0,20160816211500|cu1610,37280.0,20160816211600|cu1610,37270.0,20160816211700|cu1610,37270.00000000001,20160816211800|cu1610,37280.0,20160816211900|cu1610,37280.0,20160816212000|cu1610,37260.0,20160816212100|cu1610,37250.0,20160816212200|cu1610,37270.0,20160816212300|cu1610,37280.0,20160816212400|cu1610,37270.0,20160816212500|cu1610,37270.0,20160816212600|cu1610,37270.0,20160816212700|cu1610,37280.0,20160816212800|cu1610,37270.0,20160816212900|cu1610,37260.0,20160816213000|cu1610,37250.0,20160816213100|cu1610,37230.0,20160816213200|cu1610,37250.0,20160816213300|cu1610,37200.0,20160816213400|cu1610,37190.0,20160816213500|cu1610,37200.0,20160816213600|cu1610,37190.0,20160816213700|cu1610,37200.0,20160816213800|cu1610,37210.0,20160816213900|cu1610,37210.0,20160816214000|cu1610,37230.0,20160816214100|cu1610,37240.0,20160816214200|cu1610,37240.0,20160816214300|cu1610,37240.0,20160816214400|cu1610,37250.0,20160816214500|cu1610,37260.0,20160816214600|cu1610,37260.0,20160816214700|cu1610,37240.0,20160816214800|cu1610,37240.0,20160816214900|cu1610,37240.0,20160816215000|cu1610,37250.0,20160816215100|cu1610,37260.0,20160816215200|cu1610,37250.0,20160816215300|cu1610,37250.0,20160816215400|cu1610,37250.0,20160816215500|cu1610,37260.0,20160816215600|cu1610,37240.00000000001,20160816215700|cu1610,37240.0,20160816215800|cu1610,37260.0,20160816215900|cu1610,37250.0,20160816220000|cu1610,37270.0,20160816220100|cu1610,37280.0,20160816220200|cu1610,37280.00000000001,20160816220300|cu1610,37290.0,20160816220400|cu1610,37310.0,20160816220500|cu1610,37300.0,20160816220600|cu1610,37310.0,20160816220700|cu1610,37290.0,20160816220800|cu1610,37290.0,20160816220900|cu1610,37300.0,20160816221000|cu1610,37290.0,20160816221100|cu1610,37280.0,20160816221200|cu1610,37280.0,20160816221300|cu1610,37280.0,20160816221400|cu1610,37240.00000000001,20160816221500|cu1610,37250.0,20160816221600|cu1610,37240.0,20160816221700|cu1610,37230.0,20160816221800|cu1610,37240.0,20160816221900|cu1610,37230.0,20160816222000|cu1610,37250.0,20160816222100|cu1610,37250.0,20160816222200|cu1610,37260.0,20160816222300|cu1610,37260.0,20160816222400|cu1610,37250.0,20160816222500|cu1610,37240.0,20160816222600|cu1610,37230.0,20160816222700|cu1610,37240.0,20160816222800|cu1610,37240.0,20160816222900|cu1610,37210.0,20160816223000|cu1610,37250.0,20160816223100|cu1610,37230.00000000001,20160816223200|cu1610,37230.0,20160816223300|cu1610,37230.0,20160816223400|cu1610,37240.0,20160816223500|cu1610,37240.0,20160816223600|cu1610,37260.0,20160816223700|cu1610,37260.0,20160816223800|cu1610,37260.0,20160816223900|cu1610,37250.00000000001,20160816224000|cu1610,37250.00000000001,20160816224100|cu1610,37240.0,20160816224200|cu1610,37260.0,20160816224300|cu1610,37270.0,20160816224400|cu1610,37280.0,20160816224500|cu1610,37280.0,20160816224600|cu1610,37270.0,20160816224700|cu1610,37260.0,20160816224800|cu1610,37270.0,20160816224900|cu1610,37260.0,20160816225000|cu1610,37270.0,20160816225100|cu1610,37270.0,20160816225200|cu1610,37270.0,20160816225300|cu1610,37290.0,20160816225400|cu1610,37290.0,20160816225500|cu1610,37260.0,20160816225600|cu1610,37240.0,20160816225700|cu1610,37250.0,20160816225800|cu1610,37270.0,20160816225900|cu1610,37260.0,20160816230000|cu1610,37260.00000000001,20160816230100|cu1610,37270.0,20160816230200|cu1610,37280.0,20160816230300|cu1610,37300.00000000001,20160816230400|cu1610,37290.0,20160816230500|cu1610,37290.0,20160816230600|cu1610,37290.0,20160816230700|cu1610,37290.0,20160816230800|cu1610,37270.0,20160816230900|cu1610,37310.0,20160816231000|cu1610,37310.0,20160816231100|cu1610,37320.00000000001,20160816231200|cu1610,37310.0,20160816231300|cu1610,37320.0,20160816231400|cu1610,37350.0,20160816231500|cu1610,37350.0,20160816231600|cu1610,37360.00000000001,20160816231700|cu1610,37350.0,20160816231800|cu1610,37350.0,20160816231900|cu1610,37370.0,20160816232000|cu1610,37350.0,20160816232100|cu1610,37320.0,20160816232200|cu1610,37350.0,20160816232300|cu1610,37340.0,20160816232400|cu1610,37340.0,20160816232500|cu1610,37340.0,20160816232600|cu1610,37350.0,20160816232700|cu1610,37330.0,20160816232800|cu1610,37330.0,20160816232900|cu1610,37350.0,20160816233000|cu1610,37350.0,20160816233100|cu1610,37350.0,20160816233200|cu1610,37340.0,20160816233300|cu1610,37340.0,20160816233400|cu1610,37360.0,20160816233500|cu1610,37350.0,20160816233600|cu1610,37350.0,20160816233700|cu1610,37350.0,20160816233800|cu1610,37350.0,20160816233900|cu1610,37340.0,20160816234000|cu1610,37340.0,20160816234100|cu1610,37350.0,20160816234200|cu1610,37340.00000000001,20160816234300|cu1610,37340.0,20160816234400|cu1610,37330.0,20160816234500|cu1610,37310.0,20160816234600|cu1610,37320.0,20160816234700|cu1610,37320.00000000001,20160816234800|cu1610,37320.00000000001,20160816234900|cu1610,37320.00000000001,20160816235000|cu1610,37310.0,20160816235100|cu1610,37310.0,20160816235200|cu1610,37320.0,20160816235300|cu1610,37340.0,20160816235400|cu1610,37360.0,20160816235500|cu1610,37360.0,20160816235600|cu1610,37360.0,20160816235700|cu1610,37350.00000000001,20160816235800|cu1610,37350.0,20160816235900|cu1610,37360.0,20160817000000|cu1610,37360.0,20160817000100|cu1610,37340.0,20160817000200|cu1610,37350.0,20160817000300|cu1610,37350.0,20160817000400|cu1610,37350.0,20160817000500|cu1610,37350.0,20160817000600|cu1610,37350.0,20160817000700|cu1610,37350.0,20160817000800|cu1610,37350.0,20160817000900|cu1610,37350.0,20160817001000|cu1610,37340.0,20160817001100|cu1610,37350.0,20160817001200|cu1610,37340.0,20160817001300|cu1610,37340.0,20160817001400|cu1610,37340.0,20160817001500|cu1610,37330.00000000001,20160817001600|cu1610,37340.0,20160817001700|cu1610,37340.0,20160817001800|cu1610,37330.0,20160817001900|cu1610,37330.0,20160817002000|cu1610,37330.0,20160817002100|cu1610,37330.00000000001,20160817002200|cu1610,37340.0,20160817002300|cu1610,37340.0,20160817002400|cu1610,37340.0,20160817002500|cu1610,37340.00000000001,20160817002600|cu1610,37340.0,20160817002700|cu1610,37330.0,20160817002800|cu1610,37330.0,20160817002900|cu1610,37330.0,20160817003000|cu1610,37330.0,20160817003100|cu1610,37330.0,20160817003200|cu1610,37330.0,20160817003300|cu1610,37330.0,20160817003400|cu1610,37320.0,20160817003500|cu1610,37310.0,20160817003600|cu1610,37310.0,20160817003700|cu1610,37320.0,20160817003800|cu1610,37320.0,20160817003900|cu1610,37320.0,20160817004000|cu1610,37340.0,20160817004100|cu1610,37340.0,20160817004200|cu1610,37340.0,20160817004300|cu1610,37340.0,20160817004400|cu1610,37350.00000000001,20160817004500|cu1610,37350.0,20160817004600|cu1610,37360.0,20160817004700|cu1610,37340.00000000001,20160817004800|cu1610,37350.0,20160817004900|cu1610,37360.00000000001,20160817005000|cu1610,37350.0,20160817005100|cu1610,37340.00000000001,20160817005200|cu1610,37350.0,20160817005300|cu1610,37350.0,20160817005400|cu1610,37360.00000000001,20160817005500|cu1610,37360.00000000001,20160817005600|cu1610,37350.0,20160817005700|cu1610,37340.0,20160817005800|cu1610,37360.0,20160817005900|";

    private static final String TEST_UNSTABLE = "cu1610,37250.0,20160816222100|cu1610,37250.0,20160816222200|cu1610,37260.0,20160816222300|cu1610,37260.0,20160816222400|cu1610,37250.0,20160816222500|cu1610,37240.0,20160816222600|cu1610,37230.0,20160816222700|cu1610,37240.0,20160816222800|cu1610,37240.0,20160816222900|cu1610,37210.0,20160816223000|cu1610,37250.0,20160816223100|cu1610,37230.00000000001,20160816223200|cu1610,37230.0,20160816223300|cu1610,37230.0,20160816223400|cu1610,37240.0,20160816223500|cu1610,37240.0,20160816223600|cu1610,37260.0,20160816223700|cu1610,37260.0,20160816223800|cu1610,37260.0,20160816223900|cu1610,37250.00000000001,20160816224000|cu1610,37250.00000000001,20160816224100|cu1610,37240.0,20160816224200|cu1610,37260.0,20160816224300|cu1610,37270.0,20160816224400|cu1610,37280.0,20160816224500|cu1610,37280.0,20160816224600|cu1610,37270.0,20160816224700|cu1610,37260.0,20160816224800|cu1610,37270.0,20160816224900|cu1610,37260.0,20160816225000|cu1610,37270.0,20160816225100|cu1610,37270.0,20160816225200|cu1610,37270.0,20160816225300|cu1610,37290.0,20160816225400|cu1610,37290.0,20160816225500|cu1610,37260.0,20160816225600|cu1610,37240.0,20160816225700|cu1610,37250.0,20160816225800|cu1610,37270.0,20160816225900|cu1610,37260.0,20160816230000|cu1610,37260.00000000001,20160816230100|cu1610,37270.0,20160816230200|cu1610,37280.0,20160816230300|cu1610,37300.00000000001,20160816230400|cu1610,37290.0,20160816230500|cu1610,37290.0,20160816230600|cu1610,37290.0,20160816230700|cu1610,37290.0,20160816230800|cu1610,37270.0,20160816230900|cu1610,37310.0,20160816231000|cu1610,37310.0,20160816231100|cu1610,37320.00000000001,20160816231200|cu1610,37310.0,20160816231300|cu1610,37320.0,20160816231400|cu1610,37350.0,20160816231500|cu1610,37350.0,20160816231600|cu1610,37360.00000000001,20160816231700|cu1610,37350.0,20160816231800|cu1610,37350.0,20160816231900|cu1610,37370.0,20160816232000|cu1610,37350.0,20160816232100|cu1610,37320.0,20160816232200|cu1610,37350.0,20160816232300|cu1610,37340.0,20160816232400|cu1610,37340.0,20160816232500|cu1610,37340.0,20160816232600|cu1610,37350.0,20160816232700|cu1610,37330.0,20160816232800|cu1610,37330.0,20160816232900|cu1610,37350.0,20160816233000|cu1610,37350.0,20160816233100|cu1610,37350.0,20160816233200|cu1610,37340.0,20160816233300|cu1610,37340.0,20160816233400|cu1610,37360.0,20160816233500|cu1610,37350.0,20160816233600|cu1610,37350.0,20160816233700|cu1610,37350.0,20160816233800|cu1610,37350.0,20160816233900|cu1610,37340.0,20160816234000|cu1610,37340.0,20160816234100|cu1610,37350.0,20160816234200|cu1610,37340.00000000001,20160816234300|cu1610,37340.0,20160816234400|cu1610,37330.0,20160816234500|cu1610,37310.0,20160816234600|cu1610,37320.0,20160816234700|cu1610,37320.00000000001,20160816234800|cu1610,37320.00000000001,20160816234900|cu1610,37320.00000000001,20160816235000|cu1610,37310.0,20160816235100|cu1610,37310.0,20160816235200|cu1610,37320.0,20160816235300|cu1610,37340.0,20160816235400|cu1610,37360.0,20160816235500|cu1610,37360.0,20160816235600|cu1610,37360.0,20160816235700|cu1610,37350.00000000001,20160816235800|cu1610,37350.0,20160816235900|cu1610,37360.0,20160817000000|cu1610,37360.0,20160817000100|cu1610,37340.0,20160817000200|cu1610,37350.0,20160817000300|cu1610,37350.0,20160817000400|cu1610,37350.0,20160817000500|cu1610,37350.0,20160817000600|cu1610,37350.0,20160817000700|cu1610,37350.0,20160817000800|cu1610,37350.0,20160817000900|cu1610,37350.0,20160817001000|cu1610,37340.0,20160817001100|cu1610,37350.0,20160817001200|cu1610,37340.0,20160817001300|cu1610,37340.0,20160817001400|cu1610,37340.0,20160817001500|cu1610,37330.00000000001,20160817001600|cu1610,37340.0,20160817001700|cu1610,37340.0,20160817001800|cu1610,37330.0,20160817001900|cu1610,37330.0,20160817002000|cu1610,37330.0,20160817002100|cu1610,37330.00000000001,20160817002200|cu1610,37340.0,20160817002300|cu1610,37340.0,20160817002400|cu1610,37340.0,20160817002500|cu1610,37340.00000000001,20160817002600|cu1610,37340.0,20160817002700|cu1610,37330.0,20160817002800|cu1610,37330.0,20160817002900|cu1610,37330.0,20160817003000|cu1610,37330.0,20160817003100|cu1610,37330.0,20160817003200|cu1610,37330.0,20160817003300|cu1610,37330.0,20160817003400|cu1610,37320.0,20160817003500|cu1610,37310.0,20160817003600|cu1610,37310.0,20160817003700|cu1610,37320.0,20160817003800|cu1610,37320.0,20160817003900|cu1610,37320.0,20160817004000|cu1610,37340.0,20160817004100|cu1610,37340.0,20160817004200|cu1610,37340.0,20160817004300|cu1610,37340.0,20160817004400|cu1610,37350.00000000001,20160817004500|cu1610,37350.0,20160817004600|cu1610,37360.0,20160817004700|cu1610,37340.00000000001,20160817004800|cu1610,37350.0,20160817004900|cu1610,37360.00000000001,20160817005000|cu1610,37350.0,20160817005100|cu1610,37340.00000000001,20160817005200|cu1610,37350.0,20160817005300|cu1610,37350.0,20160817005400|cu1610,37360.00000000001,20160817005500|cu1610,37360.00000000001,20160817005600|cu1610,37350.0,20160817005700|cu1610,37340.0,20160817005800|cu1610,37360.0,20160817005900|";

    @BindView(R.id.titleBar)
    TitleBar mTitleBar;
    @BindView(R.id.tradePageHeader)
    TradePageHeader mTradePageHeader;

    @BindView(R.id.openPrice)
    TextView mOpenPrice;
    @BindView(R.id.preClosePrice)
    TextView mPreClosePrice;
    @BindView(R.id.highestPrice)
    TextView mHighestPrice;
    @BindView(R.id.lowestPrice)
    TextView mLowestPrice;
    @BindView(R.id.chartContainer)
    ChartContainer mChartContainer;

    @BindView(R.id.lastPrice)
    TextView mLastPrice;
    @BindView(R.id.priceChange)
    TextView mPriceChange;
    @BindView(R.id.buySellVolumeLayout)
    BuySellVolumeLayout mBuySellVolumeLayout;

    @BindView(R.id.buyLongBtn)
    TextView mBuyLongBtn;
    @BindView(R.id.sellShortBtn)
    TextView mSellShortBtn;

    @BindView(R.id.marketTime)
    TextView mMarketTime;
    @BindView(R.id.nextTradeTime)
    TextView mNextTradeTime;
    @BindView(R.id.marketCloseArea)
    LinearLayout mMarketCloseArea;
    @BindView(R.id.marketOpenArea)
    LinearLayout mMarketOpenArea;

    @BindView(R.id.placeOrderContainer)
    FrameLayout mPlaceOrderContainer;

    private SlidingMenu mMenu;

    private Product mProduct;
    private int mFundType;
    private List<Product> mProductList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trade);
        ButterKnife.bind(this);

        initData(getIntent());

        mTitleBar.setOnRightViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMenu.showMenu();
            }
        });

        initSlidingMenu();
        initChartView();

        // TODO: 8/20/16 测试代码,后面删除
        startScheduleJob(1000);
    }

    // TODO: 8/20/16 测试代码,后面删除
    @Override
    public void onTimeUp(int count) {
        TrendView trendView = mChartContainer.getTrendView();
        List<TrendViewData> dataList = trendView.getDataList();
        TrendView.Settings settings = trendView.getSettings();

        if (dataList == null || dataList.size() == 0) {
            return;
        }

        TrendViewData lastData = dataList.get(dataList.size() - 1);
        String date = DateUtil.addOneMinute(lastData.getDate(), TrendViewData.DATE_FORMAT);
        if (TrendView.Util.isValidDate(date, settings.getOpenMarketTimes())) {
            float lastPrice = getLastPrice();
            TrendViewData unstableData = new TrendViewData(lastData.getContractId(), lastPrice, date);
            trendView.setUnstableData(unstableData);
        }
    }

    // TODO: 8/20/16 测试代码,后面删除
    private float getLastPrice() {
        String[] data = TEST_UNSTABLE.split("[|]");
        int randomInt = Math.abs(new Random().nextInt()) % data.length;
        while (TextUtils.isEmpty(data[randomInt])) {
            randomInt = Math.abs(new Random().nextInt()) % data.length;
        }
        String[] temp = data[randomInt].split(",");
        return Float.valueOf(temp[1]);
    }

    private void initChartView() {
        TrendView.Settings settings = new TrendView.Settings();
        settings.setBaseLines(9);
        settings.setNumberScale(2);
        settings.setCalculateXAxisFromOpenMarketTime(true);
        settings.setOpenMarketTimes("09:00;10:15;10:30;11:30;13:30;15:00;21:00;01:00");
        settings.setDisplayMarketTimes("09:00;13:30;21:00;01:00");
        TrendView trendView = new TrendView(this);
        trendView.setSettings(settings);
        mChartContainer.addTrendView(trendView);
        trendView.setDataList(TrendView.Util.createDataList(TESTDATA, settings.getOpenMarketTimes()));
    }

    private void initSlidingMenu() {
        mMenu = new SlidingMenu(this);
        mMenu.setMode(SlidingMenu.RIGHT);
        mMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
        mMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        mMenu.setBehindWidthRes(R.dimen.sliding_menu_width);
        mMenu.setMenu(R.layout.sm_behind_menu);
        ListView listView = (ListView) mMenu.getMenu();
        MenuAdapter menuAdapter = new MenuAdapter(this);
        menuAdapter.addAll(mProductList);
        listView.setAdapter(menuAdapter);
    }

    private void openOrdersPage() {
        Launcher.with(this, OrderActivity.class)
                .putExtra(Product.EX_PRODUCT, mProduct)
                .putExtra(Product.EX_FUND_TYPE, mFundType)
                .execute();
    }

    private void initData(Intent intent) {
        mProduct = (Product) intent.getSerializableExtra(Product.EX_PRODUCT);
        mFundType = intent.getIntExtra(Product.EX_FUND_TYPE, 0);
        mProductList = intent.getParcelableArrayListExtra(Product.EX_PRODUCT_LIST);
    }

    @OnClick({R.id.buyLongBtn, R.id.sellShortBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buyLongBtn:
                showPlaceOrderFragment(PlaceOrderFragment.TYPE_BUY_LONG);
                break;
            case R.id.sellShortBtn:
                showPlaceOrderFragment(PlaceOrderFragment.TYPE_SELL_SHORT);
                break;
        }
    }

    private void showPlaceOrderFragment(int type) {
        mPlaceOrderContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.placeOrderContainer, PlaceOrderFragment.newInstance(type))
                .commit();
    }

    private void hidePlaceOrderFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfirmBtnClick() {
        // TODO: 8/23/16 下单接口 实现

        hidePlaceOrderFragment();
    }

    static class MenuAdapter extends ArrayAdapter<Product> {

        public MenuAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_sliding_menu, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.bindingData(getItem(position));
            return convertView;
        }

        static class ViewHolder {
            @BindView(R.id.productName)
            TextView mProductName;
            @BindView(R.id.productCode)
            TextView mProductCode;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void bindingData(Product item) {
                mProductName.setText(item.getVarietyName());
                mProductCode.setText(item.getVarietyType());
            }
        }
    }
}

package com.mahkota_company.android.customer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mahkota_company.android.NavigationDrawerCallbacks;
import com.mahkota_company.android.NavigationDrawerFragment;
import com.mahkota_company.android.R;
import com.mahkota_company.android.database.Customer;
import com.mahkota_company.android.database.DatabaseHandler;
import com.mahkota_company.android.display_product.DisplayProductActivity;
import com.mahkota_company.android.jadwal.JadwalActivity;
import com.mahkota_company.android.locator.LocatorActivity;
import com.mahkota_company.android.product.ProductActivity;
import com.mahkota_company.android.prospect.CustomerProspectActivity;
import com.mahkota_company.android.sales_order.SalesOrderActivity;
import com.mahkota_company.android.stock_on_hand.StockOnHandActivity;
import com.mahkota_company.android.utils.CONFIG;
import com.mahkota_company.android.utils.FileUtils;
import com.mahkota_company.android.utils.GlobalApp;
import com.mahkota_company.android.utils.RowItem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class CustomerActivity extends ActionBarActivity implements
		NavigationDrawerCallbacks {
	private Context act;
	private Toolbar mToolbar;
	private NavigationDrawerFragment mNavigationDrawerFragment;
	private DatabaseHandler databaseHandler;
	private ListView listview;
	private ArrayList<Customer> customer_list = new ArrayList<Customer>();
	private ListViewAdapter cAdapter;
	private ProgressDialog progressDialog;
	private Handler handler = new Handler();
	private String message;
	private String response_data;
	private static final String LOG_TAG = CustomerActivity.class
			.getSimpleName();
	private EditText searchCustomer;
	private Typeface typefaceSmall;
	private TextView tvKodeCustomer;
	private TextView tvNamaCustomer;
	private TextView tvNamaAlamat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_customer);

		mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
		typefaceSmall = Typeface.createFromAsset(getAssets(),
				"fonts/AliquamREG.ttf");
		tvKodeCustomer = (TextView) findViewById(R.id.activity_customer_title_kode_customer);
		tvNamaCustomer = (TextView) findViewById(R.id.activity_customer_title_nama_customer);
		tvNamaAlamat = (TextView) findViewById(R.id.activity_customer_title_alamat_customer);
		tvKodeCustomer.setTypeface(typefaceSmall);
		tvNamaCustomer.setTypeface(typefaceSmall);
		tvNamaAlamat.setTypeface(typefaceSmall);

		act = this;
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(getApplicationContext().getResources()
				.getString(R.string.app_name));
		progressDialog.setMessage(getApplicationContext().getResources()
				.getString(R.string.app_customer_processing));
		progressDialog.setCancelable(true);
		progressDialog.setCanceledOnTouchOutside(false);
		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(R.id.fragment_drawer);
		mNavigationDrawerFragment.setup(R.id.fragment_drawer,
				(DrawerLayout) findViewById(R.id.drawer), mToolbar);
		searchCustomer = (EditText) findViewById(R.id.activity_customer_edittext_search);
		databaseHandler = new DatabaseHandler(this);
		mNavigationDrawerFragment.selectItem(0);
		listview = (ListView) findViewById(R.id.list);
		listview.setItemsCanFocus(false);
		int countCustomer = databaseHandler.getCountCustomer();

		if (countCustomer == 0) {
			if (GlobalApp.checkInternetConnection(act)) {
				new DownloadDataCustomer().execute();
			} else {
				String message = act.getApplicationContext().getResources()
						.getString(R.string.app_customer_processing_empty);
				showCustomDialog(message);
			}
		} else {
			updateContentCustomer();
		}

		searchCustomer.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2,
					int arg3) {
				if (cs.toString().length() > 0) {
					customer_list.clear();
					SharedPreferences spPreferences = getSharedPrefereces();
					String main_app_staff_id_wilayah = spPreferences.getString(
							CONFIG.SHARED_PREFERENCES_STAFF_ID_WILAYAH, null);
					ArrayList<Customer> customer_from_db = databaseHandler.getAllCustomerActiveBaseOnSearch(
							cs.toString(),
							Integer.parseInt(main_app_staff_id_wilayah));
					if (customer_from_db.size() > 0) {
						listview.setVisibility(View.VISIBLE);
						for (int i = 0; i < customer_from_db.size(); i++) {
							int id_customer = customer_from_db.get(i)
									.getId_customer();
							String kode_customer = customer_from_db.get(i)
									.getKode_customer();
							String email = customer_from_db.get(i).getEmail();
							String alamat = customer_from_db.get(i).getAlamat();
							String lats = customer_from_db.get(i).getLats();
							String longs = customer_from_db.get(i).getLongs();
							String nama_lengkap = customer_from_db.get(i)
									.getNama_lengkap();
							String no_telp = customer_from_db.get(i)
									.getNo_telp();
							int id_wilayah = customer_from_db.get(i)
									.getId_wilayah();
							String foto1 = customer_from_db.get(i).getFoto_1();
							String foto2 = customer_from_db.get(i).getFoto_2();
							String foto3 = customer_from_db.get(i).getFoto_3();
							int id_type = customer_from_db.get(i)
									.getId_type_customer();
							String blockir = customer_from_db.get(i)
									.getBlokir();
							String date = customer_from_db.get(i).getDate();
							String status_update = customer_from_db.get(i)
									.getStatus_update();
							int id_staff = customer_from_db.get(i)
									.getId_staff();
							String no_ktp = customer_from_db.get(i)
									.getNo_ktp();
							String tanggal_lahir = customer_from_db.get(i)
									.getTanggal_lahir();
							String nama_bank = customer_from_db.get(i)
									.getNama_bank();
                            String no_rekening = customer_from_db.get(i)
                                    .getNo_rekening();
                            String atas_nama = customer_from_db.get(i)
                                    .getAtas_nama();
                            String npwp = customer_from_db.get(i)
                                    .getNpwp();
                            String nama_pasar = customer_from_db.get(i)
                                    .getNama_pasar();
                            String cluster = customer_from_db.get(i)
                                    .getCluster();
                            String telp = customer_from_db.get(i)
                                    .getTelp();
                            String fax = customer_from_db.get(i)
                                    .getFax();
                            String omset = customer_from_db.get(i)
                                    .getOmset();
                            String cara_pembayaran = customer_from_db.get(i)
                                    .getCara_pembayaran();
                            String plafon_kredit = customer_from_db.get(i)
                                    .getPlafon_kredit();
                            String term_kredit = customer_from_db.get(i)
                                    .getTerm_kredit();
							String nama_istri = customer_from_db.get(i)
									.getNama_istri();
							String nama_anak1 = customer_from_db.get(i)
									.getNama_anak1();
							String nama_anak2 = customer_from_db.get(i)
									.getNama_anak2();
							String nama_anak3 = customer_from_db.get(i)
									.getNama_anak3();

							Customer customer = new Customer();
							customer.setId_customer(id_customer);
							customer.setKode_customer(kode_customer);
							customer.setEmail(email);
							customer.setAlamat(alamat);
							customer.setLats(lats);
							customer.setLongs(longs);
							customer.setNama_lengkap(nama_lengkap);
							customer.setNo_telp(no_telp);
							customer.setId_wilayah(id_wilayah);
							customer.setFoto_1(foto1);
							customer.setFoto_2(foto2);
							customer.setFoto_3(foto3);
							customer.setId_type_customer(id_type);
							customer.setBlokir(blockir);
							customer.setDate(date);
							customer.setStatus_update(status_update);
							customer.setId_staff(id_staff);
							customer.setNo_ktp(no_ktp);
							customer.setTanggal_lahir(tanggal_lahir);
							customer.setNama_bank(nama_bank);
                            customer.setNo_rekening(no_rekening);
                            customer.setAtas_nama(atas_nama);
                            customer.setNpwp(npwp);
                            customer.setNama_pasar(nama_pasar);
                            customer.setCluster(cluster);
                            customer.setTelp(telp);
                            customer.setFax(fax);
                            customer.setOmset(omset);
                            customer.setCara_pembayaran(cara_pembayaran);
                            customer.setPlafon_kredit(plafon_kredit);
                            customer.setTerm_kredit(term_kredit);

							customer.setNama_istri(nama_istri);
							customer.setNama_anak1(nama_anak1);
							customer.setNama_anak2(nama_anak2);
							customer.setNama_anak3(nama_anak3);

							customer_list.add(customer);
						}

						cAdapter = new ListViewAdapter(CustomerActivity.this,
								R.layout.list_item_customer, customer_list);
						listview.setAdapter(cAdapter);
						cAdapter.notifyDataSetChanged();
					} else {
						listview.setVisibility(View.INVISIBLE);
					}

				} else {
					customer_list.clear();
					SharedPreferences spPreferences = getSharedPrefereces();
					String main_app_staff_id_wilayah = spPreferences.getString(
							CONFIG.SHARED_PREFERENCES_STAFF_ID_WILAYAH, null);
					ArrayList<Customer> customer_from_db = databaseHandler
							.getAllCustomerActive(Integer
									.parseInt(main_app_staff_id_wilayah));
					if (customer_from_db.size() > 0) {
						listview.setVisibility(View.VISIBLE);
						for (int i = 0; i < customer_from_db.size(); i++) {
							int id_customer = customer_from_db.get(i)
									.getId_customer();
							String kode_customer = customer_from_db.get(i)
									.getKode_customer();
							String email = customer_from_db.get(i).getEmail();
							String alamat = customer_from_db.get(i).getAlamat();
							String lats = customer_from_db.get(i).getLats();
							String longs = customer_from_db.get(i).getLongs();
							String nama_lengkap = customer_from_db.get(i)
									.getNama_lengkap();
							String no_telp = customer_from_db.get(i)
									.getNo_telp();
							int id_wilayah = customer_from_db.get(i)
									.getId_wilayah();
							String foto1 = customer_from_db.get(i).getFoto_1();
							String foto2 = customer_from_db.get(i).getFoto_2();
							String foto3 = customer_from_db.get(i).getFoto_3();
							int id_type = customer_from_db.get(i)
									.getId_type_customer();
							String blockir = customer_from_db.get(i)
									.getBlokir();
							String date = customer_from_db.get(i).getDate();
							int id_staff = customer_from_db.get(i)
									.getId_staff();
							String no_ktp = customer_from_db.get(i)
									.getNo_ktp();
							String tanggal_lahir = customer_from_db.get(i)
									.getTanggal_lahir();
							String nama_bank = customer_from_db.get(i)
									.getNama_bank();
                            String no_rekening = customer_from_db.get(i)
                                    .getNo_rekening();
                            String atas_nama = customer_from_db.get(i)
                                    .getAtas_nama();
                            String npwp = customer_from_db.get(i)
                                    .getNpwp();
                            String nama_pasar = customer_from_db.get(i)
                                    .getNama_pasar();
                            String cluster = customer_from_db.get(i)
                                    .getCluster();
                            String telp = customer_from_db.get(i)
                                    .getTelp();
                            String fax = customer_from_db.get(i)
                                    .getFax();
                            String omset = customer_from_db.get(i)
                                    .getOmset();
                            String cara_pembayaran = customer_from_db.get(i)
                                    .getCara_pembayaran();
                            String plafon_kredit = customer_from_db.get(i)
                                    .getPlafon_kredit();
                            String term_kredit = customer_from_db.get(i)
                                    .getTerm_kredit();
							String nama_istri = customer_from_db.get(i)
									.getNama_istri();
							String nama_anak1 = customer_from_db.get(i)
									.getNama_anak1();
							String nama_anak2 = customer_from_db.get(i)
									.getNama_anak2();
							String nama_anak3 = customer_from_db.get(i)
									.getNama_anak3();


							Customer customer = new Customer();
							customer.setId_customer(id_customer);
							customer.setKode_customer(kode_customer);
							customer.setEmail(email);
							customer.setAlamat(alamat);
							customer.setLats(lats);
							customer.setLongs(longs);
							customer.setNama_lengkap(nama_lengkap);
							customer.setNo_telp(no_telp);
							customer.setId_wilayah(id_wilayah);
							customer.setFoto_1(foto1);
							customer.setFoto_2(foto2);
							customer.setFoto_3(foto3);
							customer.setId_type_customer(id_type);
							customer.setBlokir(blockir);
							customer.setDate(date);
							customer.setId_staff(id_staff);
							customer.setNo_ktp(no_ktp);
							customer.setTanggal_lahir(tanggal_lahir);
							customer.setNama_bank(nama_bank);
                            customer.setNo_rekening(no_rekening);
                            customer.setAtas_nama(atas_nama);
                            customer.setNpwp(npwp);
                            customer.setNama_pasar(nama_pasar);
                            customer.setCluster(cluster);
                            customer.setTelp(telp);
                            customer.setFax(fax);
                            customer.setOmset(omset);
                            customer.setCara_pembayaran(cara_pembayaran);
                            customer.setPlafon_kredit(plafon_kredit);
                            customer.setTerm_kredit(term_kredit);
							customer.setNama_istri(nama_istri);
							customer.setNama_anak1(nama_anak1);
							customer.setNama_anak2(nama_anak2);
							customer.setNama_anak3(nama_anak3);

							customer_list.add(customer);
						}

						cAdapter = new ListViewAdapter(CustomerActivity.this,
								R.layout.list_item_customer, customer_list);
						listview.setAdapter(cAdapter);
						cAdapter.notifyDataSetChanged();
					} else {
						listview.setVisibility(View.INVISIBLE);
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});
	}

	public HttpResponse getDownloadData(String url) {
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		HttpResponse response;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			response = client.execute(get);
		} catch (UnsupportedEncodingException e1) {
			response = null;
		} catch (Exception e) {
			e.printStackTrace();
			response = null;
		}

		return response;
	}

	private class DownloadDataCustomer extends
			AsyncTask<String, Integer, String> {
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getApplicationContext().getResources()
					.getString(R.string.app_customer_processing));
			progressDialog.show();
			progressDialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							String msg = getApplicationContext()
									.getResources()
									.getString(
											R.string.MSG_DLG_LABEL_SYNRONISASI_DATA_CANCEL);
							showCustomDialog(msg);
						}
					});
		}

		@Override
		protected String doInBackground(String... params) {
			String download_data_url = CONFIG.CONFIG_APP_URL_PUBLIC
					+ CONFIG.CONFIG_APP_URL_DOWNLOAD_CUSTOMER;
			HttpResponse response = getDownloadData(download_data_url);
			int retCode = (response != null) ? response.getStatusLine()
					.getStatusCode() : -1;
			if (retCode != 200) {
				message = act.getApplicationContext().getResources()
						.getString(R.string.MSG_DLG_LABEL_URL_NOT_FOUND);
				handler.post(new Runnable() {
					public void run() {
						showCustomDialog(message);
					}
				});
			} else {
				try {
					response_data = EntityUtils.toString(response.getEntity());

					SharedPreferences spPreferences = getSharedPrefereces();
					String main_app_table_data = spPreferences.getString(
							CONFIG.SHARED_PREFERENCES_TABLE_CUSTOMER, null);
					if (main_app_table_data != null) {
						if (main_app_table_data.equalsIgnoreCase(response_data)) {
							saveAppDataCustomerSameData(act
									.getApplicationContext().getResources()
									.getString(R.string.app_value_true));
						} else {
							databaseHandler.deleteTableCustomer();
							saveAppDataCustomerSameData(act
									.getApplicationContext().getResources()
									.getString(R.string.app_value_false));
						}
					} else {
						databaseHandler.deleteTableCustomer();
						saveAppDataCustomerSameData(act.getApplicationContext()
								.getResources()
								.getString(R.string.app_value_false));
					}
				} catch (ParseException e) {
					message = e.toString();
					handler.post(new Runnable() {
						public void run() {
							showCustomDialog(message);
						}
					});
				} catch (IOException e) {
					message = e.toString();
					handler.post(new Runnable() {
						public void run() {
							showCustomDialog(message);
						}
					});
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (response_data != null) {
				saveAppDataCustomer(response_data);
				extractDataCustomer();
				message = act
						.getApplicationContext()
						.getResources()
						.getString(
								R.string.MSG_DLG_LABEL_SYNRONISASI_DATA_SUCCESS);
				showCustomDialogDownloadSuccess(message);
			} else {
				message = act.getApplicationContext().getResources()
						.getString(R.string.MSG_DLG_LABEL_DOWNLOAD_FAILED);
				handler.post(new Runnable() {
					public void run() {
						showCustomDialog(message);
					}
				});
			}
		}

	}

	public void showCustomDialogDownloadSuccess(String msg) {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				act);
		alertDialogBuilder
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton(
						act.getApplicationContext().getResources()
								.getString(R.string.MSG_DLG_LABEL_OK),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								AlertDialog alertDialog = alertDialogBuilder
										.create();
								alertDialog.dismiss();
								updateContentRefreshCustomer();
							}
						});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();

	}

	public void extractDataCustomer() {
		SharedPreferences spPreferences = getSharedPrefereces();
		String main_app_table_same_data = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_TABLE_CUSTOMER_SAME_DATA, null);
		String main_app_table = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_TABLE_CUSTOMER, null);
		if (main_app_table_same_data.equalsIgnoreCase(act
				.getApplicationContext().getResources()
				.getString(R.string.app_value_false))) {
			JSONObject oResponse;
			try {
				oResponse = new JSONObject(main_app_table);
				JSONArray jsonarr = oResponse.getJSONArray("customer");
				for (int i = 0; i < jsonarr.length(); i++) {
					JSONObject oResponsealue = jsonarr.getJSONObject(i);
					String id_customer = oResponsealue.isNull("id_customer") ? null
							: oResponsealue.getString("id_customer");
					String kode_customer = oResponsealue
							.isNull("kode_customer") ? null : oResponsealue
							.getString("kode_customer");
					String email = oResponsealue.isNull("email") ? null
							: oResponsealue.getString("email");
					String alamat = oResponsealue.isNull("alamat") ? null
							: oResponsealue.getString("alamat");
					String lats = oResponsealue.isNull("lats") ? null
							: oResponsealue.getString("lats");
					String longs = oResponsealue.isNull("longs") ? null
							: oResponsealue.getString("longs");
					String nama_lengkap = oResponsealue.isNull("nama_lengkap") ? null
							: oResponsealue.getString("nama_lengkap");
					String no_telp = oResponsealue.isNull("no_telp") ? null
							: oResponsealue.getString("no_telp");
					String id_wilayah = oResponsealue.isNull("id_wilayah") ? null
							: oResponsealue.getString("id_wilayah");
					String foto_1 = oResponsealue.isNull("foto_1") ? null
							: oResponsealue.getString("foto_1");
					String foto_2 = oResponsealue.isNull("foto_2") ? null
							: oResponsealue.getString("foto_2");
					String foto_3 = oResponsealue.isNull("foto_3") ? null
							: oResponsealue.getString("foto_3");
					String id_type_customer = oResponsealue
							.isNull("id_type_customer") ? null : oResponsealue
							.getString("id_type_customer");
					String blokir = oResponsealue.isNull("blokir") ? null
							: oResponsealue.getString("blokir");
					String date = oResponsealue.isNull("date") ? null
							: oResponsealue.getString("date");
					String id_staff = oResponsealue.isNull("id_staff") ? null
							: oResponsealue.getString("id_staff");
					String no_ktp = oResponsealue.isNull("no_ktp") ? null
							: oResponsealue.getString("no_ktp");
					String tanggal_lahir = oResponsealue.isNull("tanggal_lahir") ? null
							: oResponsealue.getString("tanggal_lahir");
					String nama_bank = oResponsealue.isNull("nama_bank") ? null
							: oResponsealue.getString("nama_bank");
					String no_rekening = oResponsealue.isNull("no_rekening") ? null
							: oResponsealue.getString("no_rekening");
					String atas_nama = oResponsealue.isNull("atas_nama") ? null
							: oResponsealue.getString("atas_nama");
					String npwp = oResponsealue.isNull("npwp") ? null
							: oResponsealue.getString("npwp");
                    String nama_pasar = oResponsealue.isNull("nama_pasar") ? null
                            : oResponsealue.getString("nama_pasar");
                    String cluster = oResponsealue.isNull("cluster") ? null
                            : oResponsealue.getString("cluster");
                    String telp = oResponsealue.isNull("telp") ? null
                            : oResponsealue.getString("telp");
                    String fax = oResponsealue.isNull("fax") ? null
                            : oResponsealue.getString("fax");
                    String omset = oResponsealue.isNull("omset") ? null
                            : oResponsealue.getString("omset");
                    String cara_pembayaran = oResponsealue.isNull("cara_pembayaran") ? null
                            : oResponsealue.getString("cara_pembayaran");
                    String plafon_kredit = oResponsealue.isNull("plafon_kredit") ? null
                            : oResponsealue.getString("plafon_kredit");
                    String term_kredit = oResponsealue.isNull("term_kredit") ? null
                            : oResponsealue.getString("term_kredit");
					String nama_istri = oResponsealue.isNull("nama_istri") ? null
							: oResponsealue.getString("nama_istri");
					String nama_anak1 = oResponsealue.isNull("nama_anak1") ? null
							: oResponsealue.getString("nama_anak1");
					String nama_anak2 = oResponsealue.isNull("nama_anak2") ? null
							: oResponsealue.getString("nama_anak2");
					String nama_anak3 = oResponsealue.isNull("nama_anak3") ? null
							: oResponsealue.getString("nama_anak3");

					Log.d(LOG_TAG, "id_customer:" + id_customer);
					Log.d(LOG_TAG, "kode_customer:" + kode_customer);
					Log.d(LOG_TAG, "email:" + email);
					Log.d(LOG_TAG, "alamat:" + alamat);
					Log.d(LOG_TAG, "lats:" + lats);
					Log.d(LOG_TAG, "longs:" + longs);
					Log.d(LOG_TAG, "nama_lengkap:" + nama_lengkap);
					Log.d(LOG_TAG, "no_telp:" + no_telp);
					Log.d(LOG_TAG, "id_wilayah:" + id_wilayah);
					Log.d(LOG_TAG, "foto_1:" + foto_1);
					Log.d(LOG_TAG, "foto_2:" + foto_2);
					Log.d(LOG_TAG, "foto_3:" + foto_3);
					Log.d(LOG_TAG, "fotid_type_customero:" + id_type_customer);
					Log.d(LOG_TAG, "blokir:" + blokir);
					Log.d(LOG_TAG, "date:" + date);
					Log.d(LOG_TAG, "id_staff:" + id_staff);
					Log.d(LOG_TAG, "no_ktp:" + no_ktp);
					Log.d(LOG_TAG, "tanggal_lahir:" + tanggal_lahir);
					Log.d(LOG_TAG, "nama_bank:" + nama_bank);
					Log.d(LOG_TAG, "no_rekening:" + no_rekening);
					Log.d(LOG_TAG, "atas_nama:" + atas_nama);
					Log.d(LOG_TAG, "npwp:" + npwp);
                    Log.d(LOG_TAG, "nama_pasar:" + nama_pasar);
                    Log.d(LOG_TAG, "cluster:" + cluster);
                    Log.d(LOG_TAG, "telp:" + telp);
                    Log.d(LOG_TAG, "fax:" + fax);
                    Log.d(LOG_TAG, "omset:" + omset);
                    Log.d(LOG_TAG, "cara_pembayaran:" + cara_pembayaran);
                    Log.d(LOG_TAG, "plafon_kredit:" + plafon_kredit);
                    Log.d(LOG_TAG, "term_kredit:" + term_kredit);
					Log.d(LOG_TAG, "nama_istri:" + nama_istri);
					Log.d(LOG_TAG, "nama_anak1:" + nama_anak1);
					Log.d(LOG_TAG, "nama_anak2:" + nama_anak2);
					Log.d(LOG_TAG, "nama_anak3:" + nama_anak3);


					databaseHandler.add_Customer(new Customer(Integer
							.parseInt(id_customer), kode_customer, email,
							alamat, lats, longs, nama_lengkap, no_telp, Integer
									.parseInt(id_wilayah), foto_1, foto_2,
							foto_3, Integer.parseInt(id_type_customer), blokir,
							date, "1", Integer.parseInt(id_staff), no_ktp,
							tanggal_lahir,nama_bank,no_rekening, atas_nama, npwp,
                            nama_pasar, cluster, telp, fax, omset, cara_pembayaran,
							plafon_kredit,term_kredit, nama_istri, nama_anak1,
							nama_anak2, nama_anak3));

				}
			} catch (JSONException e) {
				final String message = e.toString();
				handler.post(new Runnable() {
					public void run() {
						showCustomDialog(message);
					}
				});

			}
		}
	}

	public void saveAppDataCustomer(String responsedata) {
		SharedPreferences sp = getSharedPrefereces();
		Editor editor = sp.edit();
		editor.putString(CONFIG.SHARED_PREFERENCES_TABLE_CUSTOMER, responsedata);
		editor.commit();
	}

	public void saveAppDataCustomerSameData(String responsedata) {
		SharedPreferences sp = getSharedPrefereces();
		Editor editor = sp.edit();
		editor.putString(CONFIG.SHARED_PREFERENCES_TABLE_CUSTOMER_SAME_DATA,
				responsedata);
		editor.commit();
	}

	public void saveAppDataCustomerIdCustomer(String responsedata) {
		SharedPreferences sp = getSharedPrefereces();
		Editor editor = sp.edit();
		editor.putString(CONFIG.SHARED_PREFERENCES_TABLE_CUSTOMER_ID_CUSTOMER,
				responsedata);
		editor.commit();
	}

	private SharedPreferences getSharedPrefereces() {
		return act.getSharedPreferences(CONFIG.SHARED_PREFERENCES_NAME,
				Context.MODE_PRIVATE);
	}

	public void updateContentCustomer() {
		customer_list.clear();
		SharedPreferences spPreferences = getSharedPrefereces();
		String main_app_staff_id_wilayah = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_STAFF_ID_WILAYAH, null);

		String main_app_staff_level = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_STAFF_LEVEL, null);
		int levelStaff = Integer.parseInt(main_app_staff_level);
		ArrayList<Customer> customer_from_db = null;
		if (levelStaff > 2) {
			customer_from_db = databaseHandler.getAllCustomerActive(Integer
					.parseInt(main_app_staff_id_wilayah));
		} else {
			customer_from_db = databaseHandler.getAllCustomerActive();
		}

		if (customer_from_db.size() > 0) {
			listview.setVisibility(View.VISIBLE);
			for (int i = 0; i < customer_from_db.size(); i++) {
				int id_customer = customer_from_db.get(i).getId_customer();
				String kode_customer = customer_from_db.get(i)
						.getKode_customer();
				String email = customer_from_db.get(i).getEmail();
				String alamat = customer_from_db.get(i).getAlamat();
				String lats = customer_from_db.get(i).getLats();
				String longs = customer_from_db.get(i).getLongs();
				String nama_lengkap = customer_from_db.get(i).getNama_lengkap();
				String no_telp = customer_from_db.get(i).getNo_telp();
				int id_wilayah = customer_from_db.get(i).getId_wilayah();
				String foto1 = customer_from_db.get(i).getFoto_1();
				String foto2 = customer_from_db.get(i).getFoto_2();
				String foto3 = customer_from_db.get(i).getFoto_3();
				int id_type = customer_from_db.get(i).getId_type_customer();
				String blockir = customer_from_db.get(i).getBlokir();
				String date = customer_from_db.get(i).getDate();
				String status_update = customer_from_db.get(i)
						.getStatus_update();
				int id_staff = customer_from_db.get(i).getId_staff();
				String no_ktp = customer_from_db.get(i).getNo_ktp();
				String tanggal_lahir = customer_from_db.get(i).getTanggal_lahir();
				String nama_bank = customer_from_db.get(i).getNama_bank();
                String no_rekening = customer_from_db.get(i)
                        .getNo_rekening();
                String atas_nama = customer_from_db.get(i)
                        .getAtas_nama();
                String npwp = customer_from_db.get(i)
                        .getNpwp();
                String nama_pasar = customer_from_db.get(i)
                        .getNama_pasar();
                String cluster = customer_from_db.get(i)
                        .getCluster();
                String telp = customer_from_db.get(i)
                        .getTelp();
                String fax = customer_from_db.get(i)
                        .getFax();
                String omset = customer_from_db.get(i)
                        .getOmset();
                String cara_pembayaran = customer_from_db.get(i)
                        .getCara_pembayaran();
                String plafon_kredit = customer_from_db.get(i)
                        .getPlafon_kredit();
                String term_kredit = customer_from_db.get(i)
                        .getTerm_kredit();

				String nama_istri = customer_from_db.get(i)
						.getOmset();
				String nama_anak1 = customer_from_db.get(i)
						.getNama_anak1();
				String nama_anak2 = customer_from_db.get(i)
						.getNama_anak2();
				String nama_anak3 = customer_from_db.get(i)
						.getNama_anak3();

				Customer customer = new Customer();
				customer.setId_customer(id_customer);
				customer.setKode_customer(kode_customer);
				customer.setEmail(email);
				customer.setAlamat(alamat);
				customer.setLats(lats);
				customer.setLongs(longs);
				customer.setNama_lengkap(nama_lengkap);
				customer.setNo_telp(no_telp);
				customer.setId_wilayah(id_wilayah);
				customer.setFoto_1(foto1);
				customer.setFoto_2(foto2);
				customer.setFoto_3(foto3);
				customer.setId_type_customer(id_type);
				customer.setBlokir(blockir);
				customer.setDate(date);
				customer.setStatus_update(status_update);
				customer.setId_staff(id_staff);
				customer.setNo_ktp(no_ktp);
				customer.setTanggal_lahir(tanggal_lahir);
				customer.setNama_bank(nama_bank);
                customer.setNo_rekening(no_rekening);
                customer.setAtas_nama(atas_nama);
                customer.setNpwp(npwp);
                customer.setNama_pasar(nama_pasar);
                customer.setCluster(cluster);
                customer.setTelp(telp);
                customer.setFax(fax);
                customer.setOmset(omset);
                customer.setCara_pembayaran(cara_pembayaran);
                customer.setPlafon_kredit(plafon_kredit);
                customer.setTerm_kredit(term_kredit);

				customer.setNama_istri(nama_istri);
				customer.setNama_anak1(nama_anak1);
				customer.setNama_anak2(nama_anak2);
				customer.setNama_anak3(nama_anak3);

				if (foto1.length() > 0) {
					File dir = new File(CONFIG.getFolderPath() + "/"
							+ CONFIG.CONFIG_APP_FOLDER_CUSTOMER + "/" + foto1);
					if (!dir.exists()) {
						customer_list.add(customer);
					}
				}
			}

		} else {
			listview.setVisibility(View.INVISIBLE);
		}

		if (customer_list.size() > 0) {
			if (GlobalApp.checkInternetConnection(act)) {
				processDownloadContentCustomer();
			} else {
				String message = act.getApplicationContext().getResources()
						.getString(R.string.app_customer_processing_empty);
				showCustomDialog(message);
			}

		} else {
			showListCustomer();
		}
	}

	public void updateContentRefreshCustomer() {
		customer_list.clear();
		SharedPreferences spPreferences = getSharedPrefereces();
		String main_app_staff_id_wilayah = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_STAFF_ID_WILAYAH, null);
		ArrayList<Customer> customer_from_db = databaseHandler
				.getAllCustomerActive(Integer
						.parseInt(main_app_staff_id_wilayah));
		if (customer_from_db.size() > 0) {
			listview.setVisibility(View.VISIBLE);
			for (int i = 0; i < customer_from_db.size(); i++) {
				int id_customer = customer_from_db.get(i).getId_customer();
				String kode_customer = customer_from_db.get(i)
						.getKode_customer();
				String email = customer_from_db.get(i).getEmail();
				String alamat = customer_from_db.get(i).getAlamat();
				String lats = customer_from_db.get(i).getLats();
				String longs = customer_from_db.get(i).getLongs();
				String nama_lengkap = customer_from_db.get(i).getNama_lengkap();
				String no_telp = customer_from_db.get(i).getNo_telp();
				int id_wilayah = customer_from_db.get(i).getId_wilayah();
				String foto1 = customer_from_db.get(i).getFoto_1();
				String foto2 = customer_from_db.get(i).getFoto_2();
				String foto3 = customer_from_db.get(i).getFoto_3();
				int id_type = customer_from_db.get(i).getId_type_customer();
				String blockir = customer_from_db.get(i).getBlokir();
				String date = customer_from_db.get(i).getDate();
				String status_update = customer_from_db.get(i)
						.getStatus_update();
				int id_staff = customer_from_db.get(i).getId_staff();


				Customer customer = new Customer();
				customer.setId_customer(id_customer);
				customer.setKode_customer(kode_customer);
				customer.setEmail(email);
				customer.setAlamat(alamat);
				customer.setLats(lats);
				customer.setLongs(longs);
				customer.setNama_lengkap(nama_lengkap);
				customer.setNo_telp(no_telp);
				customer.setId_wilayah(id_wilayah);
				customer.setFoto_1(foto1);
				customer.setFoto_2(foto2);
				customer.setFoto_3(foto3);
				customer.setId_type_customer(id_type);
				customer.setBlokir(blockir);
				customer.setDate(date);
				customer.setStatus_update(status_update);
				customer.setId_staff(id_staff);

				if (foto1.length() > 0) {
					File dir = new File(CONFIG.getFolderPath() + "/"
							+ CONFIG.CONFIG_APP_FOLDER_CUSTOMER + "/" + foto1);
					if (dir.exists()) {
						dir.delete();
					}
					// if (!dir.exists()) {
					customer_list.add(customer);
					// }

				}
			}

		} else {
			listview.setVisibility(View.INVISIBLE);
		}

		if (customer_list.size() > 0) {
			if (GlobalApp.checkInternetConnection(act)) {
				processDownloadContentCustomer();
			} else {
				String message = act.getApplicationContext().getResources()
						.getString(R.string.app_customer_processing_empty);
				showCustomDialog(message);
			}

		} else {
			showListCustomer();
		}
	}

	private class DownloadContentCustomer extends
			AsyncTask<String, Integer, List<RowItem>> {
		List<RowItem> rowItems;
		int noOfURLs;

		protected List<RowItem> doInBackground(String... urls) {
			noOfURLs = urls.length;
			rowItems = new ArrayList<RowItem>();
			Bitmap map = null;
			for (String url : urls) {
				map = downloadImage(url.split("#")[0], url.split("#")[1]);
				rowItems.add(new RowItem(map));
			}
			return rowItems;
		}

		private Bitmap downloadImage(String urlString, String fileName) {
			int count = 0;
			Bitmap bitmap = null;
			URL url;
			InputStream inputStream = null;
			BufferedOutputStream outputStream = null;
			OutputStream output = null;

			File dir = new File(CONFIG.getFolderPath() + "/"
					+ CONFIG.CONFIG_APP_FOLDER_CUSTOMER);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			try {
				url = new URL(urlString);
				output = new FileOutputStream(dir + "/" + fileName);
				URLConnection connection = url.openConnection();
				int lenghtOfFile = connection.getContentLength();
				inputStream = new BufferedInputStream(url.openStream());
				ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				outputStream = new BufferedOutputStream(dataStream);
				byte data[] = new byte[512];
				long total = 0;
				while ((count = inputStream.read(data)) != -1) {
					total += count;
					publishProgress((int) ((total * 100) / lenghtOfFile));
					outputStream.write(data, 0, count);
					output.write(data, 0, count);
				}
				outputStream.flush();
				BitmapFactory.Options bmOptions = new BitmapFactory.Options();
				bmOptions.inSampleSize = 1;
				byte[] bytes = dataStream.toByteArray();
				bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
						bmOptions);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				FileUtils.close(output);
				FileUtils.close(inputStream);
				FileUtils.close(outputStream);
			}
			return bitmap;
		}

		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
			if (rowItems != null) {
				progressDialog.setMessage("Loading " + (rowItems.size() + 1)
						+ "/" + noOfURLs);
			}
		}

		protected void onPostExecute(List<RowItem> rowItems) {
			progressDialog.dismiss();
			showListCustomer();
		}
	}

	public void processDownloadContentCustomer() {
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(getApplicationContext().getResources()
				.getString(R.string.app_name));
		progressDialog.setMessage(getApplicationContext().getResources()
				.getString(R.string.app_customer_processing_download_content));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setIndeterminate(false);
		progressDialog.setMax(100);
		progressDialog.setCancelable(true);
		progressDialog.show();
		DownloadContentCustomer task = new DownloadContentCustomer();
		List<String> stringImg = new ArrayList<String>();
		for (int i = 0; i < customer_list.size(); i++) {
			String img1 = customer_list.get(i).getFoto_1()
					.replaceAll(" ", "%20");
			String download_image = CONFIG.CONFIG_APP_URL_DIR_IMG_CUSTOMER
					+ img1 + "#" + customer_list.get(i).getFoto_1();
			stringImg.add(download_image);
		}
		String[] imgArr = new String[stringImg.size()];
		imgArr = stringImg.toArray(imgArr);
		task.execute(imgArr);
	}

	public void showListCustomer() {
		customer_list.clear();
		SharedPreferences spPreferences = getSharedPrefereces();
		String main_app_staff_id_wilayah = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_STAFF_ID_WILAYAH, null);
		String main_app_staff_level = spPreferences.getString(
				CONFIG.SHARED_PREFERENCES_STAFF_LEVEL, null);
		int levelStaff = Integer.parseInt(main_app_staff_level);
		ArrayList<Customer> customer_from_db = null;
		if (levelStaff > 2) {
			customer_from_db = databaseHandler.getAllCustomerActive(Integer
					.parseInt(main_app_staff_id_wilayah));
		} else {
			customer_from_db = databaseHandler.getAllCustomerActive();
		}
		if (customer_from_db.size() > 0) {
			listview.setVisibility(View.VISIBLE);
			for (int i = 0; i < customer_from_db.size(); i++) {
				int id_customer = customer_from_db.get(i).getId_customer();
				String kode_customer = customer_from_db.get(i)
						.getKode_customer();
				String email = customer_from_db.get(i).getEmail();
				String alamat = customer_from_db.get(i).getAlamat();
				String lats = customer_from_db.get(i).getLats();
				String longs = customer_from_db.get(i).getLongs();
				String nama_lengkap = customer_from_db.get(i).getNama_lengkap();
				String no_telp = customer_from_db.get(i).getNo_telp();
				int id_wilayah = customer_from_db.get(i).getId_wilayah();
				String foto1 = customer_from_db.get(i).getFoto_1();
				String foto2 = customer_from_db.get(i).getFoto_2();
				String foto3 = customer_from_db.get(i).getFoto_3();
				int id_type = customer_from_db.get(i).getId_type_customer();
				String blockir = customer_from_db.get(i).getBlokir();
				String date = customer_from_db.get(i).getDate();
				String status_update = customer_from_db.get(i)
						.getStatus_update();
				int id_staff = customer_from_db.get(i).getId_staff();

				Customer customer = new Customer();
				customer.setId_customer(id_customer);
				customer.setKode_customer(kode_customer);
				customer.setEmail(email);
				customer.setAlamat(alamat);
				customer.setLats(lats);
				customer.setLongs(longs);
				customer.setNama_lengkap(nama_lengkap);
				customer.setNo_telp(no_telp);
				customer.setId_wilayah(id_wilayah);
				customer.setFoto_1(foto1);
				customer.setFoto_2(foto2);
				customer.setFoto_3(foto3);
				customer.setId_type_customer(id_type);
				customer.setBlokir(blockir);
				customer.setDate(date);
				customer.setStatus_update(status_update);
				customer.setId_staff(id_staff);
				customer_list.add(customer);
			}

			cAdapter = new ListViewAdapter(this, R.layout.list_item_customer,
					customer_list);
			listview.setAdapter(cAdapter);
			cAdapter.notifyDataSetChanged();
		} else {
			listview.setVisibility(View.INVISIBLE);
		}
	}

	public void showCustomDialog(String msg) {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				act);
		alertDialogBuilder
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton(
						act.getApplicationContext().getResources()
								.getString(R.string.MSG_DLG_LABEL_OK),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								AlertDialog alertDialog = alertDialogBuilder
										.create();
								alertDialog.dismiss();

							}
						});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			if (GlobalApp.checkInternetConnection(act)) {
				new DownloadDataCustomer().execute();
			} else {
				String message = act.getApplicationContext().getResources()
						.getString(R.string.app_customer_processing_empty);
				showCustomDialog(message);
			}
			return true;
		case R.id.menu_upload:
			if (GlobalApp.checkInternetConnection(act)) {
				int countUpload = databaseHandler
						.getCountCustomerWhereValidAndUpdate();
				if (countUpload == 0) {
					String message = act
							.getApplicationContext()
							.getResources()
							.getString(
									R.string.app_customer_processing_upload_no_data);
					showCustomDialog(message);
				} else {
					new UploadData().execute();
				}
			} else {
				String message = act
						.getApplicationContext()
						.getResources()
						.getString(
								R.string.app_customer_processing_upload_empty);
				showCustomDialog(message);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public HttpResponse uploadCustomer(final String url, final String lats,
									   final String longs, final String kode_customer,
									   final String nama_lengkap, final String email,
									   final String alamat, final String no_telp, final String id_wilayah//,
									   //final String foto_1, final String foto_2, final String foto_3,
									   //final String id_type_customer, final String date, final String id_staff,
									 //  final String no_ktp, final String tanggal_lahir, final String nama_bank,
									 //  final String no_rekening, final String atas_nama, final String npwp,
									 //  final String nama_pasar, final String cluster, final String telp,
									 //  final String fax, final String omset, final String cara_pembayaran,
									 //  final String plafon_kredit, final String term_kredit, final String nama_istri,
									 //  final String nama_anak1, final String nama_anak2, final String nama_anak3

									   ) {
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		HttpResponse response;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("lats", lats));
		params.add(new BasicNameValuePair("longs", longs));
		params.add(new BasicNameValuePair("kode_customer", kode_customer));
		params.add(new BasicNameValuePair("nama_lengkap", nama_lengkap));

		params.add(new BasicNameValuePair("email",email ));
		params.add(new BasicNameValuePair("alamat", alamat));
		params.add(new BasicNameValuePair("no_telp", no_telp));
		params.add(new BasicNameValuePair("id_wilayah", id_wilayah));
		//params.add(new BasicNameValuePair("foto_1", foto_1));
		//params.add(new BasicNameValuePair("foto_2", foto_2));
		//params.add(new BasicNameValuePair("foto_3", foto_3));
		/*
		params.add(new BasicNameValuePair("id_type_customer", id_type_customer));
		params.add(new BasicNameValuePair("date", date));
		params.add(new BasicNameValuePair("id_staff", id_staff));
		params.add(new BasicNameValuePair("no_ktp", no_ktp));
		params.add(new BasicNameValuePair("tanggal_lahir", tanggal_lahir));
		params.add(new BasicNameValuePair("nama_bank", nama_bank));
		params.add(new BasicNameValuePair("no_rekening", no_rekening));
		params.add(new BasicNameValuePair("atas_nama", atas_nama));
		params.add(new BasicNameValuePair("npwp", npwp));
		params.add(new BasicNameValuePair("nama_pasar", nama_pasar));
		params.add(new BasicNameValuePair("cluster", cluster));
		params.add(new BasicNameValuePair("telp", telp));
		params.add(new BasicNameValuePair("fax", fax));
		params.add(new BasicNameValuePair("omset", omset));
		params.add(new BasicNameValuePair("cara_pembayaran",  cara_pembayaran));
		params.add(new BasicNameValuePair("plafon_kredit", plafon_kredit));
		params.add(new BasicNameValuePair("term_kredit", term_kredit));
		params.add(new BasicNameValuePair("nama_istri", nama_istri));
		params.add(new BasicNameValuePair("nama_anak1", nama_anak1));
		params.add(new BasicNameValuePair("nama_anak2", nama_anak2));
		params.add(new BasicNameValuePair("nama_anak3", nama_anak3));
		*/
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params));
			response = httpclient.execute(httppost);
		} catch (UnsupportedEncodingException e1) {
			response = null;
		} catch (IOException e) {
			response = null;
		}

		return response;
	}

	public class UploadData extends AsyncTask<String, Integer, String> {
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getApplicationContext().getResources()
					.getString(R.string.app_customer_processing_upload));
			progressDialog.show();
			progressDialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							String msg = getApplicationContext()
									.getResources()
									.getString(
											R.string.MSG_DLG_LABEL_SYNRONISASI_DATA_CANCEL);
							showCustomDialog(msg);
						}
					});
		}

		@Override
		protected String doInBackground(String... params) {
			String upload_url = CONFIG.CONFIG_APP_URL_PUBLIC
					+ CONFIG.CONFIG_APP_URL_UPLOAD_CUSTOMER;

			List<Customer> dataUpload = databaseHandler
					.getAllCustomerActiveAndUpdateByUser();
			HttpResponse response = null;
			for (Customer customer : dataUpload) {
				response = uploadCustomer(upload_url,
						customer.getLats(), customer.getLongs(), customer.getKode_customer(), customer.getNama_lengkap(),
						customer.getEmail(),customer.getAlamat(),customer.getNo_telp(), String.valueOf(customer.getId_wilayah()));
						//customer.getId_type_customer(),customer.getDate(),customer.getId_staff(),customer.getNo_ktp(),
						//customer.getTanggal_lahir(),customer.getNama_bank()
			}
			int retCode = (response != null) ? response.getStatusLine()
					.getStatusCode() : -1;
			if (retCode != 200) {
				message = act.getApplicationContext().getResources()
						.getString(R.string.MSG_DLG_LABEL_URL_NOT_FOUND);
				handler.post(new Runnable() {
					public void run() {
						showCustomDialog(message);
					}
				});
			} else {
				try {
					response_data = EntityUtils.toString(response.getEntity());
				} catch (ParseException e) {
					message = e.toString();
					handler.post(new Runnable() {
						public void run() {
							showCustomDialog(message);
						}
					});
				} catch (IOException e) {
					message = e.toString();
					handler.post(new Runnable() {
						public void run() {
							showCustomDialog(message);
						}
					});
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (response_data != null) {
				initUploadCustomer();
			} else {
				final String msg = act
						.getApplicationContext()
						.getResources()
						.getString(
								R.string.app_customer_processing_upload_failed);
				handler.post(new Runnable() {
					public void run() {
						showCustomDialog(msg);
					}
				});
			}
		}

	}

	public void initUploadCustomer() {
		JSONObject oResponse;
		try {
			oResponse = new JSONObject(response_data);
			String status = oResponse.isNull("error") ? "True" : oResponse
					.getString("error");
			if (response_data.isEmpty()) {
				final String msg = act
						.getApplicationContext()
						.getResources()
						.getString(
								R.string.app_customer_processing_upload_failed);
				showCustomDialog(msg);
			} else {
				Log.d(LOG_TAG, "status=" + status);
				if (status.equalsIgnoreCase("True")) {
					final String msg = act
							.getApplicationContext()
							.getResources()
							.getString(
									R.string.app_customer_processing_upload_failed);
					showCustomDialog(msg);
				} else {
					final String msg = act
							.getApplicationContext()
							.getResources()
							.getString(
									R.string.app_customer_processing_upload_success);
					CustomDialogUploadSuccess(msg);
				}

			}

		} catch (JSONException e) {
			final String message = e.toString();
			showCustomDialog(message);

		}
	}

	public void CustomDialogUploadSuccess(String msg) {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				act);
		alertDialogBuilder
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton(
						act.getApplicationContext().getResources()
								.getString(R.string.MSG_DLG_LABEL_OK),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								AlertDialog alertDialog = alertDialogBuilder
										.create();
								alertDialog.dismiss();
								new DownloadDataCustomer().execute();
							}
						});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();

	}

	public class ListViewAdapter extends ArrayAdapter<Customer> {
		Activity activity;
		int layoutResourceId;
		Customer customerData;
		ArrayList<Customer> data = new ArrayList<Customer>();

		public ListViewAdapter(Activity act, int layoutResourceId,
				ArrayList<Customer> data) {
			super(act, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.activity = act;
			this.data = data;
			notifyDataSetChanged();
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View row = convertView;
			UserHolder holder = null;

			if (row == null) {
				LayoutInflater inflater = LayoutInflater.from(activity);

				row = inflater.inflate(layoutResourceId, parent, false);
				holder = new UserHolder();
				holder.list_kodeCustomer = (TextView) row
						.findViewById(R.id.customer_title_kode_customer);
				holder.list_namaCustomer = (TextView) row
						.findViewById(R.id.customer_title_nama_customer);
				holder.list_alamat = (TextView) row
						.findViewById(R.id.customer_title_alamat_customer);

				row.setTag(holder);
			} else {
				holder = (UserHolder) row.getTag();
			}
			customerData = data.get(position);
			holder.list_kodeCustomer.setText(customerData.getKode_customer());
			holder.list_namaCustomer.setText(customerData.getNama_lengkap());

			// Wilayah wilayah = databaseHandler.getWilayah(customerData
			// .getId_wilayah());
			holder.list_alamat.setText(customerData.getAlamat());
			holder.list_kodeCustomer.setTypeface(typefaceSmall);
			holder.list_namaCustomer.setTypeface(typefaceSmall);
			holder.list_alamat.setTypeface(typefaceSmall);
			row.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					String id_customer = String.valueOf(data.get(position)
							.getId_customer());
					saveAppDataCustomerIdCustomer(id_customer);
					gotoDetailCustomer();
				}
			});
			return row;

		}

		class UserHolder {
			TextView list_kodeCustomer;
			TextView list_namaCustomer;
			TextView list_alamat;
		}

	}

	public void gotoDetailCustomer() {
		Intent i = new Intent(this, DetailCustomerActivity.class);
		startActivity(i);
		finish();
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		if (mNavigationDrawerFragment != null) {
			if (mNavigationDrawerFragment.getCurrentSelectedPosition() != 0) {
				if (position == 1) {
					Intent intentActivity = new Intent(this,
							JadwalActivity.class);
					startActivity(intentActivity);
					finish();
				} else if (position == 2) {
					Intent intentActivity = new Intent(this,
							ProductActivity.class);
					startActivity(intentActivity);
					finish();
				} else if (position == 3) {
					Intent intentActivity = new Intent(this,
							CustomerProspectActivity.class);
					startActivity(intentActivity);
					finish();
				} else if (position == 4) {
					Intent intentActivity = new Intent(this,
							LocatorActivity.class);
					startActivity(intentActivity);
					finish();
				} else if (position == 5) {
					Intent intentActivity = new Intent(this,
							SalesOrderActivity.class);
					startActivity(intentActivity);
					finish();
				} else if (position == 6) {
					Intent intentActivity = new Intent(this,
							StockOnHandActivity.class);
					startActivity(intentActivity);
					finish();
				} else if (position == 7) {
					Intent intentActivity = new Intent(this,
							DisplayProductActivity.class);
					startActivity(intentActivity);
					finish();
				}
			}
		}

		// Toast.makeText(this, "Menu item selected -> " + position,
		// Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		if (mNavigationDrawerFragment.isDrawerOpen())
			mNavigationDrawerFragment.closeDrawer();
		else
			super.onBackPressed();
	}

	public void showCustomDialogExit() {
		String msg = getApplicationContext().getResources().getString(
				R.string.MSG_DLG_LABEL_EXIT_DIALOG);
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				act);
		alertDialogBuilder
				.setMessage(msg)
				.setCancelable(false)
				.setNegativeButton(
						getApplicationContext().getResources().getString(
								R.string.MSG_DLG_LABEL_YES),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								android.os.Process
										.killProcess(android.os.Process.myPid());

							}
						})
				.setPositiveButton(
						getApplicationContext().getResources().getString(
								R.string.MSG_DLG_LABEL_NO),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								AlertDialog alertDialog = alertDialogBuilder
										.create();
								alertDialog.dismiss();

							}
						});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			showCustomDialogExit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}

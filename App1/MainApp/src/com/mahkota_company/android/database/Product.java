package com.mahkota_company.android.database;

public class Product {
	private int id_product;
	private String nama_product;
	private String kode_product;
	private String harga_jual;
	private String stock;
	private String id_kemasan;
	private String foto;
	private String deskripsi;

	public Product() {

	}

	public Product(int id_product, String nama_product, String kode_product,
			String harga_jual, String stock, String id_kemasan, String foto,
			String deskripsi) {
		this.id_product = id_product;
		this.nama_product = nama_product;
		this.kode_product = kode_product;
		this.harga_jual = harga_jual;
		this.stock = stock;
		this.id_kemasan = id_kemasan;
		this.foto = foto;
		this.deskripsi = deskripsi;

	}

	public int getId_product() {
		return id_product;
	}

	public void setId_product(int id_product) {
		this.id_product = id_product;
	}

	public String getNama_product() {
		return nama_product;
	}

	public void setNama_product(String nama_product) {
		this.nama_product = nama_product;
	}

	public String getKode_product() {
		return kode_product;
	}

	public void setKode_product(String kode_product) {
		this.kode_product = kode_product;
	}

	public String getHarga_jual() {
		return harga_jual;
	}

	public void setHarga_jual(String harga_jual) {
		this.harga_jual = harga_jual;
	}

	public String getStock() {
		return stock;
	}

	public void setStock(String stock) {
		this.stock = stock;
	}

	public String getId_kemasan() {
		return id_kemasan;
	}

	public void setId_kemasan(String id_kemasan) {
		this.id_kemasan = id_kemasan;
	}

	public String getFoto() {
		return foto;
	}

	public void setFoto(String foto) {
		this.foto = foto;
	}

	public String getDeskripsi() {
		return deskripsi;
	}

	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}
}

package com.kadaiku.brunei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

data class Product(val id:String=UUID.randomUUID().toString(), val name:String, val price:Double, val shop:String, val category:String, val area:String)
data class CartLine(val product:Product, var qty:Int)
data class Order(val id:String=UUID.randomUUID().toString().take(8).uppercase(), val lines:List<CartLine>, val delivery:String, val total:Double)

@Composable fun money(v:Double) = Text("B$%.2f".format(v))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { KadaiKuApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun KadaiKuApp() {
    var tab by remember { mutableIntStateOf(0) }
    var products by remember { mutableStateOf(listOf(
        Product(name="Kek Coklat Premium",price=15.0,shop="Dapur Aisyah",category="Makanan",area="Gadong"),
        Product(name="Perfume Oud",price=20.0,shop="Brunei Fragrance",category="Beauty",area="Kiulap"),
        Product(name="Baju Melayu",price=35.0,shop="Kedai Kita",category="Fashion",area="Bandar"),
        Product(name="Frozen Food Combo",price=12.0,shop="Mama's Frozen",category="Makanan",area="Tutong")
    )) }
    var cart by remember { mutableStateOf(listOf<CartLine>()) }
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var search by remember { mutableStateOf("") }
    var showSeller by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val filtered = products.filter { search.isBlank() || it.name.contains(search,true) || it.shop.contains(search,true) || it.category.contains(search,true) }
    Scaffold(topBar={CenterAlignedTopAppBar(title={Text("KadaiKu 🇧🇳",fontWeight=FontWeight.Bold)})},
        bottomBar={NavigationBar { listOf("Home","Search","Cart (${cart.sumOf{it.qty}})","Profile").forEachIndexed{i,l->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={},label={Text(l)})} }}) { pad ->
        when(tab){
            0 -> Home(filtered,search,{search=it},{showSeller=true},{p->cart=addCart(cart,p)},Modifier.padding(pad))
            1 -> SearchPage(filtered,search,{search=it},{p->cart=addCart(cart,p)},Modifier.padding(pad))
            2 -> CartPage(cart,{p->cart=removeOne(cart,p)},{p->cart=addCart(cart,p)},{delivery,total->
                if(cart.isNotEmpty()){ orders=orders+Order(lines=cart.map{it.copy()},delivery=delivery,total=total); cart=listOf(); tab=3 }
            },Modifier.padding(pad))
            else -> ProfilePage(orders,{showSeller=true},Modifier.padding(pad))
        }
    }
    if(showSeller) SellerSheet(onClose={showSeller=false},onAdd={showAdd=true},products=products,onDelete={id->products=products.filterNot{it.id==id}})
    if(showAdd) AddProductDialog(onDismiss={showAdd=false},onSave={name,price,cat,area->products=products+Product(name=name,price=price,shop="Kedai Saya",category=cat,area=area);showAdd=false})
}

fun addCart(c:List<CartLine>,p:Product):List<CartLine>{val x=c.toMutableList();val i=x.indexOfFirst{it.product.id==p.id};if(i>=0)x[i]=x[i].copy(qty=x[i].qty+1)else x.add(CartLine(p,1));return x}
fun removeOne(c:List<CartLine>,p:Product):List<CartLine>{val x=c.toMutableList();val i=x.indexOfFirst{it.product.id==p.id};if(i>=0){if(x[i].qty>1)x[i]=x[i].copy(qty=x[i].qty-1)else x.removeAt(i)};return x}

@Composable fun Home(ps:List<Product>,search:String,setSearch:(String)->Unit,seller:()->Unit,add:(Product)->Unit,m:Modifier){Column(m.padding(16.dp)){Text("Selamat datang 👋",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(10.dp));SearchBox(search,setSearch);Spacer(Modifier.height(14.dp));Text("Kategori",style=MaterialTheme.typography.titleLarge);Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){listOf("🍔 Makanan","👕 Fashion","💄 Beauty","🏠 Rumah").forEach{AssistChip(onClick={},label={Text(it)})}};Spacer(Modifier.height(14.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Barang berdekatan",style=MaterialTheme.typography.titleLarge);TextButton(onClick=seller){Text("Jual barang")}};ProductList(ps,add)}}
@Composable fun SearchPage(ps:List<Product>,q:String,setQ:(String)->Unit,add:(Product)->Unit,m:Modifier){Column(m.padding(16.dp)){Text("Cari barang",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(10.dp));SearchBox(q,setQ);Spacer(Modifier.height(12.dp));ProductList(ps,add)}}
@Composable fun SearchBox(q:String,setQ:(String)->Unit){OutlinedTextField(value=q,onValueChange=setQ,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Cari barang atau kedai")})}
@Composable fun ProductList(ps:List<Product>,add:(Product)->Unit){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(ps){p->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${p.shop} • ${p.area}");Text(p.category)};Column(horizontalAlignment=Alignment.End){money(p.price);Button(onClick={add(p)}){Text("Tambah")}}}}}}}

@Composable fun CartPage(c:List<CartLine>,minus:(Product)->Unit,plus:(Product)->Unit,checkout:(String,Double)->Unit,m:Modifier){var delivery by remember{mutableStateOf("Delivery")};val subtotal=c.sumOf{it.product.price*it.qty};val fee=if(c.isEmpty())0.0 else if(delivery=="Pickup")0.0 else 3.0;Column(m.padding(16.dp)){Text("Troli",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));if(c.isEmpty())Text("Troli masih kosong") else {LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(c){x->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(x.product.name,fontWeight=FontWeight.Bold);money(x.product.price)};TextButton(onClick={minus(x.product)}){Text("−")};Text(x.qty.toString());TextButton(onClick={plus(x.product)}){Text("+")}}}}};Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=delivery=="Delivery",onClick={delivery="Delivery"},label={Text("Delivery B$3")});FilterChip(selected=delivery=="Pickup",onClick={delivery="Pickup"},label={Text("Pickup")})};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Jumlah",fontWeight=FontWeight.Bold);money(subtotal+fee)};Button(onClick={if(c.isNotEmpty())checkout(delivery,subtotal+fee)},modifier=Modifier.fillMaxWidth()){Text("Place Order")}}}}

@Composable fun ProfilePage(orders:List<Order>,seller:()->Unit,m:Modifier){Column(m.padding(16.dp)){Text("Akaun",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Button(onClick=seller,modifier=Modifier.fillMaxWidth()){Text("🏪 Seller Mode")};Spacer(Modifier.height(18.dp));Text("Pesanan",style=MaterialTheme.typography.titleLarge);if(orders.isEmpty())Text("Belum ada pesanan") else orders.reversed().forEach{Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text("Order #${it.id}",fontWeight=FontWeight.Bold);Text("${it.delivery} • ${it.lines.size} item");money(it.total)}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SellerSheet(onClose:()->Unit,onAdd:()->Unit,products:List<Product>,onDelete:(String)->Unit){ModalBottomSheet(onDismissRequest=onClose){Column(Modifier.padding(20.dp)){Text("Seller Mode",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Kedai Saya");Spacer(Modifier.height(12.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth()){Text("+ Tambah Produk")};Spacer(Modifier.height(12.dp));Text("Produk saya",style=MaterialTheme.typography.titleMedium);products.filter{it.shop=="Kedai Saya"}.forEach{p->Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text(p.name,Modifier.weight(1f));money(p.price);TextButton(onClick={onDelete(p.id)}){Text("Padam")}}};Spacer(Modifier.height(10.dp));Text("📦 Orders   •   📊 Sales   •   ⚙️ Settings");Spacer(Modifier.height(30.dp))}}}

@Composable fun AddProductDialog(onDismiss:()->Unit,onSave:(String,Double,String,String)->Unit){var n by remember{mutableStateOf("")};var pr by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Makanan")};var area by remember{mutableStateOf("Gadong")};AlertDialog(onDismissRequest=onDismiss,title={Text("Tambah Produk")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(n,{n=it},label={Text("Nama produk")});OutlinedTextField(pr,{pr=it},label={Text("Harga B$")});OutlinedTextField(cat,{cat=it},label={Text("Kategori")});OutlinedTextField(area,{area=it},label={Text("Kawasan")})}},confirmButton={Button(enabled=n.isNotBlank()&&pr.toDoubleOrNull()!=null,onClick={onSave(n,pr.toDouble(),cat,area)}){Text("Simpan")}},dismissButton={TextButton(onClick=onDismiss){Text("Batal")}})}

package com.kadaiku.brunei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    Scaffold(
        topBar={CenterAlignedTopAppBar(title={Text("KadaiKu 🇧🇳",fontWeight=FontWeight.Bold)})},
        bottomBar={NavigationBar { listOf("Home","Search","Cart (${cart.sumOf{it.qty}})","Profile").forEachIndexed{i,l->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={},label={Text(l)})} }}
    ) { pad ->
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

@Composable
fun Home(ps:List<Product>,search:String,setSearch:(String)->Unit,seller:()->Unit,add:(Product)->Unit,m:Modifier){
    LazyColumn(
        modifier=m.fillMaxSize().background(Color(0xFFF8F8FB)),
        contentPadding=PaddingValues(bottom=20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
        item {
            Box(
                modifier=Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF7B2FF7),Color(0xFFFF4E91),Color(0xFFFFA726)))
                ).padding(20.dp)
            ){
                Column {
                    Text("Selamat datang 👋",color=Color.White,style=MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Cari barang local Brunei yang best!",color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(14.dp))
                    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(18.dp)){
                        SearchBox(search,setSearch)
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal=16.dp)){
                Text("Kategori Popular",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    CategoryCard("🍔","Makanan",Color(0xFFFFE0B2),Modifier.weight(1f))
                    CategoryCard("👕","Fashion",Color(0xFFD1C4E9),Modifier.weight(1f))
                    CategoryCard("💄","Beauty",Color(0xFFFFCDD2),Modifier.weight(1f))
                    CategoryCard("🏠","Rumah",Color(0xFFC8E6C9),Modifier.weight(1f))
                }
            }
        }

        item {
            Card(
                modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth(),
                colors=CardDefaults.cardColors(containerColor=Color(0xFF1E88E5)),
                shape=RoundedCornerShape(22.dp)
            ){
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("PROMO HARI ANI 🎉",color=Color.White,fontWeight=FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Support local seller & discover barang menarik dekat kitani.",color=Color.White)
                    }
                    Text("🇧🇳",style=MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Column {
                    Text("Barang Berdekatan",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text("Pilihan seller sekitar Brunei",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
                }
                FilledTonalButton(onClick=seller){Text("+ Jual")}
            }
        }

        items(ps){ p ->
            ColorfulProductCard(p,add,Modifier.padding(horizontal=16.dp))
        }
    }
}

@Composable
fun CategoryCard(icon:String,label:String,bg:Color,m:Modifier=Modifier){
    Card(modifier=m,colors=CardDefaults.cardColors(containerColor=bg),shape=RoundedCornerShape(18.dp)){
        Column(Modifier.padding(vertical=14.dp,horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text(icon,style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(label,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ColorfulProductCard(p:Product,add:(Product)->Unit,m:Modifier=Modifier){
    val accent=when(p.category.lowercase()){
        "makanan"->Color(0xFFFFF3E0)
        "beauty"->Color(0xFFFFEBEE)
        "fashion"->Color(0xFFEDE7F6)
        else->Color(0xFFE8F5E9)
    }
    Card(modifier=m.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
            Box(
                modifier=Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(accent),
                contentAlignment=Alignment.Center
            ){
                Text(when(p.category.lowercase()){"makanan"->"🍰";"beauty"->"✨";"fashion"->"👕";else->"🛍️"},style=MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)){
                Text(p.name,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Text(p.shop,color=Color(0xFF6A1B9A),fontWeight=FontWeight.SemiBold)
                Text("📍 ${p.area}  •  ${p.category}",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
                Spacer(Modifier.height(5.dp))
                Text("B$%.2f".format(p.price),fontWeight=FontWeight.ExtraBold,color=Color(0xFFE65100),style=MaterialTheme.typography.titleMedium)
            }
            Button(onClick={add(p)},shape=RoundedCornerShape(14.dp)){Text("Tambah")}
        }
    }
}

@Composable fun SearchPage(ps:List<Product>,q:String,setQ:(String)->Unit,add:(Product)->Unit,m:Modifier){Column(m.padding(16.dp)){Text("Cari barang",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(10.dp));SearchBox(q,setQ);Spacer(Modifier.height(12.dp));ProductList(ps,add)}}
@Composable fun SearchBox(q:String,setQ:(String)->Unit){OutlinedTextField(value=q,onValueChange=setQ,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Cari barang atau kedai")},shape=RoundedCornerShape(16.dp))}
@Composable fun ProductList(ps:List<Product>,add:(Product)->Unit){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(ps){p->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${p.shop} • ${p.area}");Text(p.category)};Column(horizontalAlignment=Alignment.End){money(p.price);Button(onClick={add(p)}){Text("Tambah")}}}}}}}

@Composable fun CartPage(c:List<CartLine>,minus:(Product)->Unit,plus:(Product)->Unit,checkout:(String,Double)->Unit,m:Modifier){var delivery by remember{mutableStateOf("Delivery")};val subtotal=c.sumOf{it.product.price*it.qty};val fee=if(c.isEmpty())0.0 else if(delivery=="Pickup")0.0 else 3.0;Column(m.padding(16.dp)){Text("Troli",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));if(c.isEmpty())Text("Troli masih kosong") else {LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(c){x->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(x.product.name,fontWeight=FontWeight.Bold);money(x.product.price)};TextButton(onClick={minus(x.product)}){Text("−")};Text(x.qty.toString());TextButton(onClick={plus(x.product)}){Text("+")}}}}};Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=delivery=="Delivery",onClick={delivery="Delivery"},label={Text("Delivery B$3")});FilterChip(selected=delivery=="Pickup",onClick={delivery="Pickup"},label={Text("Pickup")})};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Jumlah",fontWeight=FontWeight.Bold);money(subtotal+fee)};Button(onClick={if(c.isNotEmpty())checkout(delivery,subtotal+fee)},modifier=Modifier.fillMaxWidth()){Text("Place Order")}}}}

@Composable fun ProfilePage(orders:List<Order>,seller:()->Unit,m:Modifier){Column(m.padding(16.dp)){Text("Akaun",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Button(onClick=seller,modifier=Modifier.fillMaxWidth()){Text("🏪 Seller Mode")};Spacer(Modifier.height(18.dp));Text("Pesanan",style=MaterialTheme.typography.titleLarge);if(orders.isEmpty())Text("Belum ada pesanan") else orders.reversed().forEach{Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text("Order #${it.id}",fontWeight=FontWeight.Bold);Text("${it.delivery} • ${it.lines.size} item");money(it.total)}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SellerSheet(onClose:()->Unit,onAdd:()->Unit,products:List<Product>,onDelete:(String)->Unit){ModalBottomSheet(onDismissRequest=onClose){Column(Modifier.padding(20.dp)){Text("Seller Mode",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Kedai Saya");Spacer(Modifier.height(12.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth()){Text("+ Tambah Produk")};Spacer(Modifier.height(12.dp));Text("Produk saya",style=MaterialTheme.typography.titleMedium);products.filter{it.shop=="Kedai Saya"}.forEach{p->Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text(p.name,Modifier.weight(1f));money(p.price);TextButton(onClick={onDelete(p.id)}){Text("Padam")}}};Spacer(Modifier.height(10.dp));Text("📦 Orders   •   📊 Sales   •   ⚙️ Settings");Spacer(Modifier.height(30.dp))}}}

@Composable fun AddProductDialog(onDismiss:()->Unit,onSave:(String,Double,String,String)->Unit){var n by remember{mutableStateOf("")};var pr by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Makanan")};var area by remember{mutableStateOf("Gadong")};AlertDialog(onDismissRequest=onDismiss,title={Text("Tambah Produk")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(n,{n=it},label={Text("Nama produk")});OutlinedTextField(pr,{pr=it},label={Text("Harga B$")});OutlinedTextField(cat,{cat=it},label={Text("Kategori")});OutlinedTextField(area,{area=it},label={Text("Kawasan")})}},confirmButton={Button(enabled=n.isNotBlank()&&pr.toDoubleOrNull()!=null,onClick={onSave(n,pr.toDouble(),cat,area)}){Text("Simpan")}},dismissButton={TextButton(onClick=onDismiss){Text("Batal")}})}

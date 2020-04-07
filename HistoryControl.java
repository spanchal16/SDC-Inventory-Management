import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Properties;

// Below is the class that implements the interface inventoryControl
public class HistoryControl implements inventoryControl {

	Connection connect = null;
	Statement statement = null;
	ResultSet customer = null;
	ResultSet order = null;
	ResultSet product = null;
	ResultSet beforePlaceOrder = null;
	ResultSet toPlaceOrder = null;
	int productUpdate = 0;
	int dateUpdate = 0;
	ResultSet multiOrders = null;

	// Below method will accept the order number as parameter
	@Override
	public void Ship_order(int orderNumber) throws OrderException {
		boolean orderPlaced = true;
		boolean discontinued = false;
		String shippedDate = null;
		ArrayList<Integer> productID = new ArrayList();
		ArrayList<Integer> quantity = new ArrayList();
		boolean orderFound = false;
		try {
			// Below method set the connection
			connection();

			// Getting the shipped date from orders table where the order id matches the
			// order number provided in parameter
			customer = statement.executeQuery("Select ShippedDate from orders where OrderID = '" + orderNumber + "';");
			// looping through all the shipped date and storing in shippedDate
			while (customer.next()) {
				shippedDate = customer.getString("ShippedDate");
				orderFound = true;
			}

			// Checks those shipped date which are null and orderFound is true.
			// Take product id and quantity of the order number from orderdetails table
			// and add all the productid and quantity into lists
			if (shippedDate == null && orderFound) {
				order = statement.executeQuery(
						"Select ProductId,Quantity from orderdetails where OrderID = '" + orderNumber + "';");
				while (order.next()) {
					int prodID = order.getInt("ProductId");
					productID.add(prodID);
					int qty = order.getInt("Quantity");
					quantity.add(qty);
				}
				// Fetching Unit in stock and dicontinued of those productid from productid list
				// storing all the discontinued and unit in stock and checking if stockCheck is less than 0 or discontinued
				// then throw the exception
				for (int i = 0; i < productID.size(); i++) {
					product = statement.executeQuery("Select UnitsInStock,Discontinued from products where ProductID = "
							+ productID.get(i) + ";");
					while (product.next()) {
						discontinued = product.getBoolean("Discontinued");
						int unitsInStock = product.getInt("UnitsInStock");
						int stockCheck = unitsInStock - quantity.get(i);
						if (stockCheck < 0 || discontinued) {
							orderPlaced = false;
							if (discontinued) {
								throw new OrderException("Order contain discontinued product", orderNumber);
							} else {
								throw new OrderException("Order can not be delivered due to insufficient quantity",
										orderNumber);
							}
						}
					}
				}
				
				if (!orderPlaced) {
					if (discontinued) {
						throw new OrderException("Order contain discontinued product", orderNumber);
					} else {
						throw new OrderException("Order can not be delivered due to insufficient quantity",
								orderNumber);
					}
				} else {
					for (int i = 0; i < productID.size(); i++) {
						int stockCheck = 0;
						product = statement.executeQuery(
								"Select UnitsInStock from products where ProductID = " + productID.get(i) + ";");
						while (product.next()) {
							int unitsInStock = product.getInt("UnitsInStock");
							stockCheck = unitsInStock - quantity.get(i);
						}
						productUpdate = statement.executeUpdate("Update products set unitsInStock = '" + stockCheck
								+ "' where productID = " + productID.get(i) + ";");
					}
					dateUpdate = statement.executeUpdate(
							"UPDATE orders SET shippedDate = CURDATE() where orderID = " + orderNumber + ";");
				}
			} else {
				if (shippedDate != null) {
					throw new OrderException("Order already delivered", orderNumber);
				} else {
					throw new OrderException("Order Not Found", orderNumber);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// Closing all the connections
			try {
				if (statement != null) {
					statement.close();
				}

				if (connect != null) {
					connect.close();
				}

				if (customer != null) {
					customer.close();
				}

				if (order != null) {
					order.close();
				}

				if (product != null) {
					product.close();
				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	ResultSet orderDateCheck = null;
	String retrivedOrderDate = null;

	// Below method will accept year, month and day as parameter
	@Override
	public int Issue_reorders(int year, int month, int day) {
		int orderID = 0;
		ArrayList<Integer> ordlist = new ArrayList<Integer>();
		LinkedHashSet<Integer> orderProductID = new LinkedHashSet<Integer>();
		ArrayList<Integer> toOrderProdcutID = new ArrayList<Integer>();
		ArrayList<Integer> supplierId = new ArrayList<Integer>();
		LinkedHashSet<Integer> suppliersId = new LinkedHashSet();
		ArrayList<Integer> quantityOrder = new ArrayList<Integer>();
		HashMap<Integer, Double> price = new HashMap();
		String date = "";
		date = year + "-" + month + "-" + day;
		// Calling the connection method which will set up the connection
		connection();
		
		
		try {
			// Fetching order date from the new created table supplierOrder of those date which is provided in parameters
			// looping through all the order date and storing it in retrivedOrderDate
			orderDateCheck = statement
					.executeQuery("Select orderDate from supplierOrder where orderDate = '" + date + "' ");
			while (orderDateCheck.next()) {
				retrivedOrderDate = orderDateCheck.getString("orderDate");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		// checking the received date with the received date if it is true,
		// then the order for that particular date has already been issued
		if (date.equalsIgnoreCase(retrivedOrderDate)) {
			System.out.println("Order for this date has already been issued");
		} else {
			ResultSet maxOrderRef = null;
			int orderRef = 1;
			try {
				// Taking the maximum of the referenced order from the supplier order table
				maxOrderRef = statement.executeQuery("Select MAX(orderRef) as maxRef from supplierOrder;");
				while (maxOrderRef.next()) {
					orderRef = maxOrderRef.getInt("maxRef");
					orderRef++;
				}
			} catch (Exception e) {

			}

			try {
				
				multiOrders = statement.executeQuery("Select OrderID from orders where ShippedDate = '" + date + "';");
				while (multiOrders.next()) {
					orderID = multiOrders.getInt("OrderId");
					ordlist.add(orderID);
				}
				for (Integer ordlist1 : ordlist) {

					int orderProduct = 0;
					beforePlaceOrder = statement.executeQuery(
							"Select ProductId,UnitPrice from orderdetails where OrderId = " + ordlist1 + ";");
					while (beforePlaceOrder.next()) {
						orderProduct = beforePlaceOrder.getInt("ProductId");
						orderProductID.add(orderProduct);

						double actualPrice = beforePlaceOrder.getDouble("UnitPrice");
						actualPrice = actualPrice / 1.15;
						actualPrice = Math.round(actualPrice * 100.0) / 100.0;
						price.put(orderProduct, actualPrice);
					}
				}
				int totalUnitsInStock = 0;
				int currentReorderLevel = 0;
				int UnitsOnOrder = 0;
				int supplierNumber = 0;
				int totalQuantityOrder = 0;

				for (Integer ordlist2 : orderProductID) {
					boolean discountinuedProduct = false;
					toPlaceOrder = statement.executeQuery(
							"Select SupplierId,UnitsInStock,UnitsOnOrder,ReorderLevel,Discontinued from products where ProductId = "
									+ ordlist2 + ";");
					while (toPlaceOrder.next()) {
						totalUnitsInStock = toPlaceOrder.getInt("UnitsInStock");
						currentReorderLevel = toPlaceOrder.getInt("ReorderLevel");
						UnitsOnOrder = toPlaceOrder.getInt("UnitsOnOrder");
						supplierNumber = toPlaceOrder.getInt("SupplierId");
						discountinuedProduct = toPlaceOrder.getBoolean("Discontinued");
						if (currentReorderLevel == 0) {
							currentReorderLevel = 5;
						}
						if (totalUnitsInStock <= currentReorderLevel && UnitsOnOrder == 0 && !discountinuedProduct) {
							totalQuantityOrder = (currentReorderLevel * 2) - totalUnitsInStock;
							toOrderProdcutID.add(ordlist2);
							supplierId.add(supplierNumber);
							quantityOrder.add(totalQuantityOrder);
						}
					}
				}

				int supplierOrder = 0;
				int supplierOrderDetails = 0;
				int orderCount = 0;
				int UOS_Update = 0;
				ResultSet orderReceived = null;
				String supplierName = "";
				int totalsupplierId = 0;

				if (toOrderProdcutID.isEmpty()) {
					return suppliersId.size();
				} else {
					for (int i = 0; i < toOrderProdcutID.size(); i++) {
						if (orderCount == 0) {
							supplierOrder = statement
									.executeUpdate("INSERT INTO supplierOrder (orderRef, orderDate) VALUES (" + orderRef
											+ ", '" + date + "');");
						}
						orderCount++;
				
						supplierOrderDetails = statement.executeUpdate(
								"INSERT INTO supplierOrderDetails (orderRef, productId, supplierID,quantityOrdered,price) VALUES ("
										+ orderRef + ", " + toOrderProdcutID.get(i) + ", " + supplierId.get(i) + ", "
										+ quantityOrder.get(i) + ", " + price.get(toOrderProdcutID.get(i)) + ");");
						UOS_Update = statement.executeUpdate("UPDATE products SET UnitsOnOrder = "
								+ quantityOrder.get(i) + " WHERE ProductID = " + toOrderProdcutID.get(i) + ";");
						orderReceived = statement.executeQuery(
								"select supplierId from supplierOrderDetails where orderRef =" + orderRef + ";");
						while (orderReceived.next()) {
							totalsupplierId = orderReceived.getInt("supplierId");
							suppliersId.add(totalsupplierId);
						}
					}

					return suppliersId.size();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return 0;
	}

	ResultSet receiveOrder = null;
	LinkedHashSet<Integer> productReceived = new LinkedHashSet<Integer>();
	int productReceivedId = 0;
	int updateUnitsInStock = 0;
	int unitsToTransfer = 0;
	int unitsAlreadyAvalible = 0;
	int totalUnits = 0;
	ResultSet shiftUnitsOnOrder = null;
	int arrivedDate = 0;
	String arrivedDateCheck = null;
	ResultSet checkOrderReceived = null;

	// Below method will take reference order as argument
	@Override
	public void Receive_order(int internal_order_reference) throws OrderException {
		// Below method will create connection
		connection();

		boolean orderRefExist = false;
		try {
			// Selecting the arrived date from supplierOrder table 
			checkOrderReceived = statement.executeQuery(
					"Select arrivedDate from supplierOrder where orderRef = " + internal_order_reference + ";");
			while (checkOrderReceived.next()) {
				arrivedDateCheck = checkOrderReceived.getString("arrivedDate");
				orderRefExist = true;
			}
			if (arrivedDateCheck == null && orderRefExist) {
				receiveOrder = statement.executeQuery("Select productId from supplierOrderDetails where orderRef = "
						+ internal_order_reference + ";");
				while (receiveOrder.next()) {
					productReceivedId = receiveOrder.getInt("productId");
					productReceived.add(productReceivedId);
				}
				for (Integer prod : productReceived) {
					shiftUnitsOnOrder = statement.executeQuery(
							"Select UnitsInStock,UnitsOnOrder from products where ProductId = " + prod + ";");
					while (shiftUnitsOnOrder.next()) {
						unitsAlreadyAvalible = shiftUnitsOnOrder.getInt("UnitsInStock");
						unitsToTransfer = shiftUnitsOnOrder.getInt("UnitsOnOrder");
						totalUnits = unitsAlreadyAvalible + unitsToTransfer;
					}
					updateUnitsInStock = statement.executeUpdate("UPDATE products SET UnitsInStock = " + totalUnits
							+ ", UnitsOnOrder = '0' WHERE productID = " + prod + ";");
					arrivedDate = statement
							.executeUpdate("UPDATE supplierOrder SET arrivedDate = curdate() where orderRef = "
									+ internal_order_reference + ";");
				}
			} else {
				if (!orderRefExist) {
					throw new OrderException("Order Ref does not exist", internal_order_reference);
				} else {
					throw new OrderException("Order has already been received", internal_order_reference);
				}

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// This method is used to connect to the database by creating object of Properties class so that we can get the fields 
	// stored in file
	private void connection() {

	
		Properties identity = new Properties();

		try {
			File f = new File("my_info");
			FileInputStream input = new FileInputStream(f);
			identity.load(input);

		} catch (Exception e) {
			System.out.println("Not Found");
		}

		// Get the info for logging into the database.

		String user = identity.getProperty("user");
		String password = identity.getProperty("password");
		String database = identity.getProperty("database");

		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.cj.jdbc.Driver");

			// Setup the connection with the DB
			connect = DriverManager.getConnection(
					"jdbc:mysql://db.cs.dal.ca:3306/spanchal?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
					user, password);

			// Statements allow to issue SQL queries to the database. Create an instance
			// that we will use to ultimately send queries to the database.
			statement = connect.createStatement();

			// Choose a database to use
			statement.executeQuery("use " + database + ";");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

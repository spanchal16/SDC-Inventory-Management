CREATE TABLE `supplierOrder` (
`orderRef` int(11) NOT NULL,
`orderDate` varchar(45) DEFAULT NULL,
`arrivedDate` varchar(45) DEFAULT NULL,
PRIMARY KEY (`orderRef`)
);

CREATE TABLE `supplierOrderDetails` (
`orderRef` int(11) NOT NULL,
`productId` int(11) NOT NULL,
`supplierId` int(11) DEFAULT NULL,
`quantityOrdered` int(11) DEFAULT NULL,
PRIMARY KEY (`orderRef`,`productId`)
);
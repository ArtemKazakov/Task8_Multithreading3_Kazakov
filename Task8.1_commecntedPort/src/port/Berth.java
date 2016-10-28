package port;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import warehouse.Container;
import warehouse.Warehouse;

public class Berth {

	private int id;
	private Warehouse portWarehouse;

	public Berth(int id, Warehouse warehouse) {
		this.id = id;
		portWarehouse = warehouse;
	}

	public int getId() {
		return id;
	}

	public boolean add(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException {
		boolean result = false;
		Lock portWarehouseLock = portWarehouse.getLock();	
		boolean portLock = false;

		try{
			portLock = portWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (portLock) {
				/*
				Ошибка заключается в том, что новое количество контейнеров расчитывается путём
				прибавления количства добавлемых контейнеров к свободному размеру скалада порта и сравнивается
				со свободным местом на этом складе. Таким образом, когда склад будет заполнен на половину в
				него нельзя будет добавить новые контейнеры.
				 */
				int newConteinerCount = portWarehouse.getRealSize()	+ numberOfConteiners;
				if (newConteinerCount <= portWarehouse.getFreeSize()) {
					result = doMoveFromShip(shipWarehouse, numberOfConteiners);	
				}
			}
		} finally{
			if (portLock) {
				portWarehouseLock.unlock();
			}
		}

		return result;
	}
	
	private boolean doMoveFromShip(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException{
		Lock shipWarehouseLock = shipWarehouse.getLock();
		boolean shipLock = false;
		
		try{
			/*
			В идеале можно не брать блок на корабль, т.к. при данной логике приложения идет выгрузка контейнеров
			со склада кораблся на защищенный склад порта. А к конкретному складу кораблся никто обратиться
			больше не сможет, но это не критично.
			 */
			shipLock = shipWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (shipLock) {
				if(shipWarehouse.getRealSize() >= numberOfConteiners){
					List<Container> containers = shipWarehouse.getContainer(numberOfConteiners);
					portWarehouse.addContainer(containers);
					return true;
				}
			}
		}finally{
			if (shipLock) {
				shipWarehouseLock.unlock();
			}
		}
		
		return false;		
	}

	public boolean get(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException {
		boolean result = false;
		Lock portWarehouseLock = portWarehouse.getLock();	
		boolean portLock = false;

		try{
			portLock = portWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (portLock) {
				if (numberOfConteiners <= portWarehouse.getRealSize()) {
					result = doMoveFromPort(shipWarehouse, numberOfConteiners);	
				}
			}
		} finally{
			if (portLock) {
				portWarehouseLock.unlock();
			}
		}

		return result;
	}
	
	private boolean doMoveFromPort(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException{
		Lock shipWarehouseLock = shipWarehouse.getLock();
		boolean shipLock = false;
		
		try{
			/*
			Про блокировку коробля аналогично с методом doMoveFromShip()
			 */
			shipLock = shipWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (shipLock) {
				/*
				Ошибка заключается в том, что новое количество контейнеров расчитывается путём
				прибавления количства забираемых контейнеров к свободному размеру скалада коробля и сравнивается
				со свободным местом на этом складе. Таким образом, когда склад будет заполнен на половину в
				него нельяз будет забрать новыее контейнеры из порта.
				 */
				int newConteinerCount = shipWarehouse.getRealSize() + numberOfConteiners;
				if(newConteinerCount <= shipWarehouse.getFreeSize()){
					List<Container> containers = portWarehouse.getContainer(numberOfConteiners);
					shipWarehouse.addContainer(containers);
					return true;
				}
			}
		}finally{
			if (shipLock) {
				shipWarehouseLock.unlock();
			}
		}
		
		return false;		
	}
}

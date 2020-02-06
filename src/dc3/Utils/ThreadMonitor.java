class ThreadMonitor{
	private ConcurrentLinkedQueue<Thread> queue;
	private int timems;
	private class Monitor extends Thread{
		ConcurrentLinkedQueue<Thread> queue;
		public Semaphore sem;
		public Monitor(ConcurrentLinkedQueue<Thread> q){
			queue=q;
			sem=new Semaphore(1);
		}
		public void run(){
			while(sem.tryAcquire()){
				Thread t=queue.poll();
				if(t!=null)t.start();
				try{
					TimeUnit.MILLISECONDS.sleep(timems);
				}catch(InterruptedException e){
					break;
				}
				sem.release();
			}
		}
	}
	private Monitor monitor;
	ThreadMonitor(int timems){
		this.timems=timems;
		queue=new ConcurrentLinkedQueue<Thread>();
		monitor=new Monitor(queue);
		monitor.start();
	}
	public void add(Thread t){
		queue.add(t);
	}
	public void stop()throws InterruptedException{
		monitor.sem.acquire();
	}
}

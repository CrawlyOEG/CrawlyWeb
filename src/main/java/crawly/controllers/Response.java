package crawly.controllers;

public class Response {
		private String status;
		private double porcentage[];
		//0 en caso correcto, 1 en el contrario	
		private int numero;
		public Response() {
			
		}
		public Response(String status) {
			super();
			this.status = status;
		}
		public Response(String status, double porcentage[],int numero) {
			super();
			this.status = status;
			this.porcentage = porcentage;
			this.numero = numero;
		}
		public int getNumero() {
			return numero;
		}
		public void setNumero(int numero) {
			this.numero = numero;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public double[] getPorcentage() {
			return porcentage;
		}
		public void setPorcentage(double[] porcentage) {
			this.porcentage = porcentage;
		} 
}

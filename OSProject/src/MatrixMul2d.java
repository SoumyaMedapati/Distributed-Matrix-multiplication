import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import mpi.MPI;

public class MatrixMul2d {
	public static void main(String[] args) throws IOException {
		// System.out.println(args[0]+"  args[0]  "+args[1]);
		long[][] Matrix1 = readFile(new String(
				"C:/example/MPJEclipseTest/first.txt"));
		long[][] Matrix2 = readFile(new String(
				"C:/example/MPJEclipseTest/second.txt"));
		long[][] Matrixi3;
		int N = 1500;
		int MASTER = 0;
		int FROM_MASTER = 1;
		int FROM_WORKER = 2;
		int numtasks, /* number of tasks in partition */
		taskid, /* a task identifier */
		numworkers, /* number of worker tasks */
		source, /* task id of message source */
		dest, /* task id of message destination */
		nbytes, /* number of bytes in message */
		mtype, /* message type */
		intsize, /* size of an integer in bytes */
		dbsize, /* size of a double float in bytes */
		averow, extra, /* used to determine rows sent to each worker */
		// i, j, k, /* misc */
		count;
		int NRA = Matrix1.length;
		int NRB = Matrix2.length;
		int NCA = Matrix1[0].length;
		int NCB = Matrix2[0].length;
		long resultMatrix[][] = new long[NRA][NCB];
		// int Matrix1[][] = new int[NRA][NCA]; /* matrix A to be multiplied */
		// int[][] Matrix2 = new int[NRB][NCB]; /* matrix B to be multiplied */
		int[][] c = new int[4][4]; /* result matrix C */
		int[] offset = new int[1];
		int[] rows = new int[1]; /* rows of matrix A sent to each worker */
		long x[][];// = new int[2][4];
		long[] computeTime = new long[1];
		long[] maxComputeTime = new long[1];
		MPI.Init(args);
		taskid = MPI.COMM_WORLD.Rank();
		numtasks = MPI.COMM_WORLD.Size();
		numworkers = numtasks - 1;
		if (Matrix1[0].length == Matrix2.length) {
			/*
			 * Matrixi3 is the resultant matrix. Its size is row count of matrix
			 * 1 * column count of matrix 2.
			 */
			Matrixi3 = new long[Matrix1.length][Matrix2[0].length];
		}
		/*
		 * System.out.println("printing matrix 1***********"); for (int i = 0; i
		 * < NRA; i++) { for (int j = 0; j < NCA; j++) { // Matrix1[i][j] = i +
		 * j; System.out.print(Matrix1[i][j] + " "); } System.out.println(""); }
		 * System.out.println("printing matrix 2***********"); for (int i = 0; i
		 * < NRB; i++) { for (int j = 0; j < NCB; j++) { // Matrix1[i][j] = i +
		 * j; System.out.print(Matrix2[i][j] + " "); } System.out.println(""); }
		 */
		/* *************** Master Task ****************** */
		if (taskid == MASTER) {
			// Init matrix A,B

			// Send matrix data to worker tasks
			long start = System.currentTimeMillis();
			averow = NRA / numworkers;
			extra = NRA % numworkers;
			offset[0] = 0;
			mtype = FROM_MASTER;

			long startsend = System.currentTimeMillis();
			for (dest = 1; dest <= numworkers; dest++) {
			//	System.out.println("started sending woker task..");
				if (dest <= extra) {
					rows[0] = averow + 1;
				} else {
					rows[0] = averow;
				}
				MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, dest, mtype);
				MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, dest, mtype);
				count = rows[0] * NCA;
				int off = offset[0];
				x = new long[rows[0]][NCA];
				int sum = off + rows[0];
				int p = 0;
//				System.out.println("started constructing matrix rows[0] =="
//						+ rows[0]);
				for (int w = 0; w < rows[0]; w++) {
					for (int z = 0; z < NCA; z++) {
						x[w][z] = Matrix1[off + w][z];
					}
					// p++;
				}

				/*
				 * System.out.println("contructed matrix a******"); for (int v =
				 * 0; v < Matrix1.length; v++) { for (int ww = 0; ww <
				 * Matrix1[0].length; ww++) { System.out.print(Matrix1[v][ww] +
				 * " "); } System.out.println(""); }
				 * 
				 * System.out.println("contructed matrix x &&&"); for (int v =
				 * 0; v < x.length; v++) { for (int ww = 0; ww < x[0].length;
				 * ww++) { System.out.print(x[v][ww] + " "); }
				 * System.out.println(""); } System.out.println("length done ");
				 */
				Object[] sendObj = new Object[1];
				sendObj[0] = (Object) x;
				Object[] sendObj2 = new Object[1];
				sendObj2[0] = (Object) Matrix2;

				MPI.COMM_WORLD.Send(sendObj, 0, 1, MPI.OBJECT, dest, mtype);
				count = NCA * NCB;
				MPI.COMM_WORLD.Send(sendObj2, 0, 1, MPI.OBJECT, dest, mtype);
//				System.out.println("sednding done " + offset[0]);
				offset[0] = offset[0] + rows[0];
			}
			long stopsend = System.currentTimeMillis();
			// Wait for results from all worker tasks
			computeTime[0] = 0;
			mtype = FROM_WORKER;
			for (int i = 1; i <= numworkers; i++) {
				source = i;
				MPI.COMM_WORLD.Recv(computeTime, 0, 1, MPI.LONG, source, mtype);
				System.out.println("Node " + i + " uses " + computeTime[0]
						+ "ms for computing");
				MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, mtype);
				MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, mtype);
				count = rows[0] * NCB;
				// MPI.COMM_WORLD.Recv(c[offset[0]][0], 0, count, MPI.INT,
				// source,
				// mtype);
				Object[] recObjResult = new Object[1];
				MPI.COMM_WORLD.Recv(recObjResult, 0, 1, MPI.OBJECT, source,
						mtype);
				long c1[][] = (long[][]) recObjResult[0];
				// System.out
				// .println(rows[0]
				// +
				// "= rows[0] c with math recevied matrix in worker task offset= "
				// + offset[0]);
				int tow = offset[0];
				for (int i1 = 0; i1 < rows[0]; i1++) {
					// if (tow < rows[0]) {
					for (int j1 = 0; j1 < c1[0].length; j1++) {
						resultMatrix[tow][j1] = c1[i1][j1];
						// System.out.print(resultMatrix[tow][j1] + " fuck ");
					}

					tow++;
					// }
					// System.out.println("");
				}
			}
			long stop = System.currentTimeMillis();
			// System.out.println("Result of matrix c[0] = " + c[0] +
			// ", c[1000*1000] = " + c[100*100]);
			// for(int x=0;x<c.length;x++)
			/*
			 * for (int x1 = 0; x1 < resultMatrix.length; x1++) { for (int y =
			 * 0; y < resultMatrix[0].length; y++) {
			 * System.out.print(resultMatrix[x1][y] + " "); }
			 * System.out.println(""); }
			 */

			StringBuilder outputData = new StringBuilder();
			for (int i = 0; i < resultMatrix.length; i++) {
				for (int j = 0; j < resultMatrix[0].length; j++) {
					outputData.append(resultMatrix[i][j]);
					if (j == resultMatrix[0].length - 1) {
					} else {
						outputData.append(",");
					}
					// System.out.print(Matrixi3[i][j] + " ");
				}
				// System.out.println("");
				// System.out.println("row id "+i);
				outputData.append("\n");
			}
			FileWriter out = new FileWriter("output.txt");
			PrintWriter print1 = new PrintWriter(out);
			print1.append(outputData);
			print1.close();
			System.out.println("**********************************************");
			System.out.println("Number of allocated Nodes "+numtasks);
			System.out.println("Number of working Nodes "+numworkers);
			System.out.println("Time Usage = " + (stop - start)+" ms");
			System.out
					.println("Sending Time Usage = " + (stopsend - startsend)+" ms");

		}

		/*
		 * *************************** worker task
		 * ***********************************
		 */
		if (taskid > MASTER) {
			mtype = FROM_MASTER;
			source = MASTER;
			// int x1[][] = new int[2][4];
			// for (int i1 = 0; i1 < 2; i1++) {
			// for (int j1 = 0; j1 < 4; j1++) {
			// x1[i1][j1] = 0;
			// }
			// }
			// int b1[][] = new int[4][4];
			// for (int i1 = 0; i1 < 4; i1++) {
			// for (int j1 = 0; j1 < 4; j1++) {
			// b1[i1][j1] = 0;
			// }
			// }
			MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, mtype);
			MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, mtype);
			count = rows[0] * NCA;
			Object[] recObj = new Object[1];
			Object[] recObj2 = new Object[1];
			MPI.COMM_WORLD.Recv(recObj, 0, 1, MPI.OBJECT, source, mtype);
			long[][] x1 = (long[][]) recObj[0];
			count = NCA * NCB;
//			System.out.println("work tast x1");
//			for (int i1 = 0; i1 < x1.length; i1++) {
//				for (int j1 = 0; j1 < x1[0].length; j1++) {
//					System.out.print(x1[i1][j1] + " ");
//				}
//				System.out.println("");
//			}
			MPI.COMM_WORLD.Recv(recObj2, 0, 1, MPI.OBJECT, source, mtype);
			long[][] b1 = (long[][]) recObj2[0];
//			System.out.println("work tast b1");
//			for (int i1 = 0; i1 < b1.length; i1++) {
//				for (int j1 = 0; j1 < b1[0].length; j1++) {
//					System.out.print(b1[i1][j1] + " ");
//				}
//				System.out.println("");
//			}
			long startCompute = System.currentTimeMillis();
			// for (i = 0; i < NCB; i++) {
			// for (k = 0; k < 4; k++) {
			// c[i][k] = 0;
			// for (j = 0; j < NCA; j++) {
			// // c[(i*N)+k] = c[(i*N)+k] + a[(i*N)+j] * b[(j*N)+k];
			// c[i][k] = c[i][k] + x1[i][j] * b1[j][k];
			// }
			// }
			// }

			// for (k = 0; k < 4; k++) {
			// for (i = 0; i < rows[0]; i++) {
			// c[i][k] = 0;
			// for (j = 0; j < 4; j++) {
			// c[i][k] = c[i][k] + x1[i][j] * b1[j][k];
			// }
			// System.out.println("i="+i+", j="+k + c[i][k]);
			// }
			// }
			long[][] newResult = new long[x1.length][b1[0].length];

			/*
			 * Loop through each and get product, then sum up and store the
			 * value
			 */
			for (int i2 = 0; i2 < x1.length; i2++) {
				for (int j2 = 0; j2 < b1[0].length; j2++) {
					for (int k2 = 0; k2 < x1[0].length; k2++) {
						newResult[i2][j2] += x1[i2][k2] * b1[k2][j2];
					}
				}
			}

//			System.out.println("matrix c for each task");
//			for (int i1 = 0; i1 < newResult.length; i1++) {
//				for (int j1 = 0; j1 < newResult[0].length; j1++) {
//					System.out.print(newResult[i1][j1] + " ");
//				}
//				System.out.println("");
//			}
//			System.out.println("done here");
			long stopCompute = System.currentTimeMillis();
			computeTime[0] = (stopCompute - startCompute);
			mtype = FROM_WORKER;
			MPI.COMM_WORLD.Send(computeTime, 0, 1, MPI.LONG, MASTER, mtype);
			MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, MASTER, mtype);
			MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, MASTER, mtype);
			Object result[] = new Object[1];
			result[0] = (Object) newResult;
			// MPI.COMM_WORLD.Send(c, 0, rows[0] * NCB, MPI.INT, MASTER, mtype);
			MPI.COMM_WORLD.Send(result, 0, 1, MPI.OBJECT, MASTER, mtype);
		}

		MPI.COMM_WORLD.Reduce(computeTime, 0, maxComputeTime, 0, 1, MPI.LONG,
				MPI.MAX, 0);
		if (taskid == 0) {
//			System.out.println("Max compute time/machine = "
//					+ maxComputeTime[0]+" ms/machine" );
		}

		MPI.Finalize();
	}

	public static long[][] readFile(String fname) {
		String Filepath = fname;
		String Delimiter = ",";
		String line;
		BufferedReader br = null;
		long[][] Matrixi = null;
		/*
		 * Logic to read the data line by line and split them based on comma. We
		 * calculate the row count and column count to allocate memory.
		 */
		try {
			br = new BufferedReader(new FileReader(Filepath));
			int rowno = 0, j = 0, colno = 0;
			while ((line = br.readLine()) != null) {
				String row[] = line.split(Delimiter);
				rowno++;
				colno = row.length;
			}
			/*
			 * Variable rowno gives the row count in input file. Variable colno
			 * gives the col count in input file.
			 */
			Matrixi = new long[rowno][colno];
			br.close();
			br = new BufferedReader(new FileReader(Filepath));
			rowno = 0;
			/*
			 * This logic converts the character value of input file to integer
			 * value of matrix
			 */
			while ((line = br.readLine()) != null) {
				String row[] = line.split(Delimiter);
				for (j = 0; j < colno; j++) {
					Matrixi[rowno][j] = Integer.parseInt(row[j]);
				}
				rowno++;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		return Matrixi; /* Returns the Matrix of input file */
	}
}